/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.forge.mixin.core.world.entity.player;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.impl.entity.AbstractModifierEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.inventory.PlayerInventoryTransaction;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.DamageEventUtil;
import org.spongepowered.forge.mixin.core.world.entity.LivingEntityMixin_Forge_Attack_impl;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
@Mixin(value = Player.class, priority = 900)
public abstract class PlayerMixin_Forge_Attack_Impl extends LivingEntityMixin_Forge_Attack_impl {

    //@formatter:off
    @Shadow @Final public InventoryMenu inventoryMenu;
    @Shadow public abstract float shadow$getAttackStrengthScale(final float $$0);

    //@formatter:on

    private void impl$playAttackSound(Player thisPlayer, SoundEvent sound) {
        if (this.bridge$vanishState().createsSounds()) {
            thisPlayer.level().playSound(null, thisPlayer.getX(), thisPlayer.getY(), thisPlayer.getZ(), sound, thisPlayer.getSoundSource());
        }
    }

    private DamageEventUtil.Attack<Player> forgeAttackImpl$attack;
    private AttackEntityEvent forgeAttackImpl$attackEvent;
    private Map<String, Double> forgeAttackImpl$finalDamageAmounts;

    private int forgeAttackImpl$attackStrengthTicker;
    private boolean forgeAttackImpl$isStrongSprintAttack;

    /**
     * Cleanup
     */
    @Inject(method = "attack", at = @At("RETURN"))
    public void attackImpl$onReturnCleanup(final Entity $$0, final CallbackInfo ci) {
        this.forgeAttackImpl$attack = null;
        this.forgeAttackImpl$attackEvent = null;
        this.forgeAttackImpl$finalDamageAmounts = null;
    }

    /**
     * Captures the base damage for the {@link AttackEntityEvent} in {@link #forgeAttackImpl$attack}
     * and the {@link #attackStrengthTicker} in case we need to roll it back.
     * Reset {@link #forgeAttackImpl$isStrongSprintAttack}
     */
    @Inject(method = "attack", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F", shift = At.Shift.BEFORE))
    public void attackImpl$captureAttackStart(final Entity target, final CallbackInfo ci, final float baseDamage, final ItemStack weapon, final DamageSource source) {
        final var strengthScale = this.shadow$getAttackStrengthScale(0.5F);
        this.forgeAttackImpl$attack = new DamageEventUtil.Attack<>((Player) (Object) this, target, weapon, source, strengthScale, baseDamage, new ArrayList<>());
        this.forgeAttackImpl$attackStrengthTicker = this.attackStrengthTicker;
        this.forgeAttackImpl$isStrongSprintAttack = false;
    }

    /**
     * Captures the enchantment damage calculations as functions
     */
    @Inject(method = "attack", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    public void attackImpl$enchanttDamageFunc(final Entity $$0, final CallbackInfo ci) {
        final var weapon = this.forgeAttackImpl$attack.weapon();
        // this.getEnchantedDamage(targetEntity, damage, damageSource) - damage;
        final var functions = DamageEventUtil.createAttackEnchantmentFunction(weapon, this.forgeAttackImpl$attack.target(), this.forgeAttackImpl$attack.dmgSource());
        final var separateFunc = DamageEventUtil.provideSeparateEnchantmentFromBaseDamageFunction(this.forgeAttackImpl$attack.baseDamage(), weapon);
        // enchantmentDamage *= attackStrength;
        final var strengthScaleFunc = DamageEventUtil.provideCooldownEnchantmentStrengthFunction(weapon, this.forgeAttackImpl$attack.strengthScale());

        this.forgeAttackImpl$attack.functions().addAll(functions);
        this.forgeAttackImpl$attack.functions().add(separateFunc);
        this.forgeAttackImpl$attack.functions().add(strengthScaleFunc);
    }

    /**
     * Captures the attack-strength damage scaling as a function
     */
    @Inject(method = "attack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V"))
    public void attackImpl$attackStrengthScalingDamageFunc(final Entity $$0, final CallbackInfo ci) {
        // damage *= 0.2F + attackStrength * attackStrength * 0.8F;
        final var strengthScaleFunc = DamageEventUtil.provideCooldownAttackStrengthFunction((Player) (Object) this,  this.forgeAttackImpl$attack.strengthScale());
        this.forgeAttackImpl$attack.functions().add(strengthScaleFunc);
    }

    /**
     *  Prevents the {@link SoundEvents#PLAYER_ATTACK_KNOCKBACK} from playing before the event.
     *  Captures if {@link #forgeAttackImpl$isStrongSprintAttack} for later
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    public void attackImpl$preventSprintingAttackSound(final Level instance, final Player $$0, final double $$1, final double $$2, final double $$3, final SoundEvent $$4,
                                                       final SoundSource $$5, final float $$6, final float $$7) {
        // prevent sound
        this.forgeAttackImpl$isStrongSprintAttack = true;
    }

    /**
     * Captures the weapon bonus damage as a function
     */
    @Inject(method = "attack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    public void attackImpl$attackDamageFunc(final Entity $$0, final CallbackInfo ci) {
        // damage += weaponItem.getItem().getAttackDamageBonus(targetEntity, damage, damageSource);

        final var bonusDamageFunc = DamageEventUtil.provideWeaponAttackDamageBonusFunction( this.forgeAttackImpl$attack.target(),  this.forgeAttackImpl$attack.weapon(),  this.forgeAttackImpl$attack.dmgSource());
        this.forgeAttackImpl$attack.functions().add(bonusDamageFunc);
    }

    /**
     * Crit Hook - Before vanilla decides
     * Also captures the crit multiplier as a function
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z", ordinal = 1),
            to = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;walkDist:F")),
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/entity/player/CriticalHitEvent;getDamageModifier()F")
    )
    public float attackImpl$critHook(final CriticalHitEvent forgeEvent) {
        final var bonusDamageFunc = DamageEventUtil.provideCriticalAttackFunction(this.forgeAttackImpl$attack.sourceEntity(), forgeEvent.getDamageModifier());
        this.forgeAttackImpl$attack.functions().add(bonusDamageFunc);
        return forgeEvent.getDamageModifier();
    }

    /**
     * Capture damageSource for sweep attacks event later
     * Calculate knockback earlier than vanilla for event
     * call the AttackEntityEvent
     * Play prevented sound from {@link #attackImpl$preventSprintingAttackSound}
     * returns false if canceled, appearing for vanilla as an invulnerable target. {@link #attackImpl$onNoDamageSound}
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    public boolean attackImpl$onHurt(final Entity targetEntity, final DamageSource damageSource, final float mcDamage) {

        float knockbackModifier = this.shadow$getKnockback(targetEntity, damageSource) + (this.forgeAttackImpl$isStrongSprintAttack ? 1.0F : 0.0F);
        this.forgeAttackImpl$attackEvent = DamageEventUtil.callPlayerAttackEntityEvent(this.forgeAttackImpl$attack, knockbackModifier);

        if (this.forgeAttackImpl$attackEvent.isCancelled()) {
            // TODO this is actually not really doing anything because a ServerboundSwingPacket also resets it immediatly after
            this.attackStrengthTicker = this.forgeAttackImpl$attackStrengthTicker; // Reset to old value
            return false;
        }

        this.forgeAttackImpl$finalDamageAmounts = AbstractModifierEvent.finalAmounts(this.forgeAttackImpl$attackEvent.originalDamage(), this.forgeAttackImpl$attackEvent.modifiers());

        if (this.forgeAttackImpl$isStrongSprintAttack) {
            // Play prevented sprint attack sound
            this.impl$playAttackSound(this.forgeAttackImpl$attack.sourceEntity(), SoundEvents.PLAYER_ATTACK_KNOCKBACK);
        }

        return targetEntity.hurt(damageSource, (float) this.forgeAttackImpl$attackEvent.finalOutputDamage());
    }

    /**
     * Set enchantment damage with value from event
     */
    @ModifyVariable(method = "attack", ordinal = 1,
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V", ordinal = 0)),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F"))
    public float attackImpl$enchentmentDamageFromEvent(final float enchDmg) {
        return this.forgeAttackImpl$finalDamageAmounts.getOrDefault(ResourceKey.minecraft("attack_enchantment"), 0.0).floatValue();
    }

    /**
     *  Redirects Player#getKnockback to the attack event value
     */
    @Redirect(method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getKnockback(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F"))
    public float attackImpl$sweepHook(final Player instance, final Entity entity, final DamageSource damageSource) {
        return this.forgeAttackImpl$attackEvent.knockbackModifier();
    }

    /**
     * Prevents the {@link SoundEvents#PLAYER_ATTACK_NODAMAGE} when event was canceled
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    public void attackImpl$onNoDamageSound(final Level instance, final Player $$0, final double $$1, final double $$2, final double $$3,
                                           final SoundEvent $$4, final SoundSource $$5, final float $$6, final float $$7) {
        if (!this.forgeAttackImpl$attackEvent.isCancelled()) {
            this.impl$playAttackSound($$0, SoundEvents.PLAYER_ATTACK_NODAMAGE);
        }
    }


    /**
     * Call Sweep Attack Events
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"))
    public double attackImpl$beforeSweepHurt(final Player instance, final Entity sweepTarget) {
        final var distanceToSqr = instance.distanceToSqr(sweepTarget);
        if (!(distanceToSqr < 9.0)) {
            return distanceToSqr; // Too far - no event
        }

        final var mainAttack = this.forgeAttackImpl$attack;
        final var mainAttackDamage = this.forgeAttackImpl$finalDamageAmounts.getOrDefault(ResourceKey.minecraft("attack_damage"), 0.0).floatValue();

        var sweepAttack = new DamageEventUtil.Attack<>(mainAttack.sourceEntity(), sweepTarget, mainAttack.weapon(), mainAttack.dmgSource(), mainAttack.strengthScale(), 1, new ArrayList<>());
        // float sweepBaseDamage = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * attackDamage;
        sweepAttack.functions().add(DamageEventUtil.provideSweepingDamageRatioFunction(mainAttack.weapon(), mainAttack.sourceEntity(), mainAttackDamage));
        // float sweepFullDamage = this.getEnchantedDamage(sweepTarget, sweepBaseDamage, $$3) * strengthScale;
        sweepAttack.functions().addAll(DamageEventUtil.createAttackEnchantmentFunction(mainAttack.weapon(), sweepTarget, mainAttack.dmgSource()));
        sweepAttack.functions().add(DamageEventUtil.provideCooldownEnchantmentStrengthFunction(mainAttack.weapon(), mainAttack.strengthScale()));

        this.forgeAttackImpl$attackEvent = DamageEventUtil.callPlayerAttackEntityEvent(sweepAttack, 1.0F);
        if (forgeAttackImpl$attackEvent.isCancelled()) {
            return Double.MAX_VALUE;
        }

        return distanceToSqr;
    }

    /**
     * Redirect Player#getEnchantedDamage to sweep event value
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    public float attackImpl$beforeSweepHurt(final Player instance, final Entity $$0, final float $$1, final DamageSource $$2) {
        return (float) this.forgeAttackImpl$attackEvent.finalOutputDamage();
    }

    /**
     * Redirect {@link LivingEntity#knockback} to use modified event knockback
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    public void attackImpl$modifyKnockback(final LivingEntity instance, final double $$0, final double $$1, final double $$2) {
        instance.knockback($$0 * this.forgeAttackImpl$attackEvent.knockbackModifier(), $$1, $$2);
    }


    /**
     * Prevent vanilla {@link net.minecraft.world.entity.boss.EnderDragonPart#parentMob} resolution
     * We use {@link #attackImpl$parentPartsHook} instead
     */
    @Redirect(method = "attack",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setLastHurtMob(Lnet/minecraft/world/entity/Entity;)V"),
            to = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/boss/EnderDragonPart;parentMob:Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;")),
        at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/entity/boss/EnderDragonPart", ordinal = 0))
    public boolean attackImpl$parentPartsHookInstanceOf(final Object instance, final Class<?> type) {
        return false;
    }

    /**
     * Hook parent part resolution
     */
    // TODO - Remove once https://github.com/MinecraftForge/MinecraftForge/pull/10011 is merged
    @ModifyVariable(method = "attack", ordinal = 1,
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/boss/EnderDragonPart;parentMob:Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtEnemy(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/player/Player;)Z")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;"))
    public Entity attackImpl$parentPartsHook(final Entity entity) {
        if (entity instanceof PartEntity<?> pe) {
            return pe.getParent();
        }
        return entity;
    }

    /**
     * Captures inventory changes
     */
    @Redirect(method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    public void attackImpl$causeInventoryCapture(final Player instance, final InteractionHand interactionHand, final ItemStack stack) {
        instance.setItemInHand(interactionHand, stack);

        // Capture...
        final PhaseContext<@NonNull ?> context = PhaseTracker.SERVER.getPhaseContext();
        final TransactionalCaptureSupplier transactor = context.getTransactor();
        transactor.logPlayerInventoryChange(instance, PlayerInventoryTransaction.EventCreator.STANDARD);
        this.inventoryMenu.broadcastChanges();
    }

    @Redirect(method = "actuallyHurt", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isInvulnerableTo(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    public boolean attackImpl$startActuallyHurt(final Player instance, final DamageSource damageSource, final DamageSource $$0, final float originalDamage) {
        if (instance.isInvulnerableTo(damageSource)) {
            return true;
        }
        // Call platform hook for adjusting damage
        final var modAdjustedDamage = this.bridge$applyModDamage(instance, damageSource, originalDamage);
        // TODO check for direct call?
        this.attackImpl$actuallyHurt = new DamageEventUtil.ActuallyHurt(instance, new ArrayList<>(), damageSource, modAdjustedDamage);
        return false;
    }

    /**
     * Set final damage after calling {@link Player#setAbsorptionAmount} in which we called the event
     */
    @ModifyVariable(method = "actuallyHurt", ordinal = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setAbsorptionAmount(F)V", shift = At.Shift.AFTER))
    public float attackImpl$setFinalDamage(final float value) {
        if (this.attackImpl$actuallyHurtResult.event().isCancelled()) {
            return 0;
        }
        return this.attackImpl$actuallyHurtFinalDamage;
    }

    /**
     * Set absorbed damage after calling {@link Player#setAbsorptionAmount} in which we called the event
     */
    @ModifyVariable(method = "actuallyHurt", ordinal = 2,
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setAbsorptionAmount(F)V")),
        at = @At(value = "STORE", ordinal = 0))
    public float attackImpl$setAbsorbed(final float value) {
        if (this.attackImpl$actuallyHurtResult.event().isCancelled()) {
            return 0;
        }
        return this.attackImpl$actuallyHurtResult.damageAbsorbed().orElse(0f);
    }

    /**
     * Cleanup
     */
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    public void attackImpl$afterActuallyHurt(final DamageSource $$0, final float $$1, final CallbackInfo ci) {
        this.attackImpl$actuallyHurt = null;
        this.attackImpl$actuallyHurtResult = null;
    }


}