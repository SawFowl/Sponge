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
package org.spongepowered.common.data.provider.entity;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.minecart.MinecartCommandBlockEntity;
import net.minecraft.entity.monster.BlazeEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.HandPreference;
import org.spongepowered.api.util.Color;
import org.spongepowered.common.bridge.entity.AggressiveEntityBridge;
import org.spongepowered.common.bridge.entity.item.ItemEntityBridge;
import org.spongepowered.common.data.provider.DataProviderRegistry;
import org.spongepowered.common.data.provider.DataProviderRegistryBuilder;
import org.spongepowered.common.data.provider.entity.areaeffectcloud.AreaEffectCloudEntityParticleEffectProvider;
import org.spongepowered.common.data.provider.entity.armorstand.ArmorStandEntityBodyRotationsProvider;
import org.spongepowered.common.data.provider.entity.armorstand.ArmorStandEntityPlacingDisabledProvider;
import org.spongepowered.common.data.provider.entity.armorstand.ArmorStandEntityRotationProvider;
import org.spongepowered.common.data.provider.entity.armorstand.ArmorStandEntityTakingDisabledProvider;
import org.spongepowered.common.data.provider.entity.base.EntityDisplayNameProvider;
import org.spongepowered.common.data.provider.entity.base.EntityFireDamageDelayProvider;
import org.spongepowered.common.data.provider.entity.base.EntityInvisibleProvider;
import org.spongepowered.common.data.provider.entity.base.EntityInvulnerabilityTicksProvider;
import org.spongepowered.common.data.provider.entity.horse.AbstractHorseEntityIsSaddledProvider;
import org.spongepowered.common.data.provider.entity.horse.HorseEntityHorseColorProvider;
import org.spongepowered.common.data.provider.entity.horse.HorseEntityHorseStyleProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityActiveItemProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityBodyRotationsProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityChestRotationProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityHeadRotationProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityHealthProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityLastAttackerProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityMaxAirProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityMaxHealthProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityPotionEffectsProvider;
import org.spongepowered.common.data.provider.entity.living.LivingEntityRemainingAirProvider;
import org.spongepowered.common.data.provider.entity.player.PlayerEntityExhaustionProvider;
import org.spongepowered.common.data.provider.entity.player.PlayerEntityFlyingSpeedProvider;
import org.spongepowered.common.data.provider.entity.player.PlayerEntityFoodLevelProvider;
import org.spongepowered.common.data.provider.entity.player.PlayerEntitySaturationProvider;
import org.spongepowered.common.data.provider.entity.player.PlayerEntityWalkingSpeedProvider;
import org.spongepowered.common.data.provider.entity.user.UserFirstDatePlayedProvider;
import org.spongepowered.common.data.provider.entity.user.UserLastDatePlayedProvider;
import org.spongepowered.common.data.provider.entity.vanishable.VanishableEntityVanishIgnoresCollisionProvider;
import org.spongepowered.common.data.provider.entity.vanishable.VanishableEntityVanishPreventsTargetingProvider;
import org.spongepowered.common.data.provider.entity.vanishable.VanishableEntityVanishProvider;
import org.spongepowered.common.data.provider.entity.wolf.WolfEntityIsWetProvider;
import org.spongepowered.common.data.util.PotionEffectHelper;
import org.spongepowered.common.mixin.accessor.entity.AreaEffectCloudEntityAccessor;
import org.spongepowered.common.mixin.accessor.entity.MobEntityAccessor;
import org.spongepowered.common.mixin.accessor.entity.item.ArmorStandEntityAccessor;
import org.spongepowered.common.mixin.accessor.entity.item.FallingBlockEntityAccessor;
import org.spongepowered.common.mixin.accessor.entity.monster.BlazeEntityAccessor;
import org.spongepowered.common.mixin.accessor.entity.monster.CreeperEntityAccessor;
import org.spongepowered.common.mixin.accessor.tileentity.CommandBlockLogicAccessor;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;

public class EntityDataProviders extends DataProviderRegistryBuilder {

    public EntityDataProviders(DataProviderRegistry registry) {
        super(registry);
    }

    @Override
    protected void register() {
        register(new ArrowEntityPotionEffectsProvider());
        register(new BoatEntityWoodTypeProvider());
        register(new MobEntityDominantHandProvider());

        // Other
        register(BlazeEntity.class, Keys.IS_AFLAME.get(),
                BlazeEntity::isBurning,
                (accessor, value) -> ((BlazeEntityAccessor) accessor).accessor$setOnFire(value));

        register(AbstractArrowEntity.class, Keys.CRITICAL_HIT.get(),
                AbstractArrowEntity::getIsCritical,
                AbstractArrowEntity::setIsCritical);

        register(AggressiveEntityBridge.class, Keys.IS_ANGRY.get(),
                AggressiveEntityBridge::bridge$isAngry,
                AggressiveEntityBridge::bridge$setAngry);

        register(PigEntity.class, Keys.IS_SADDLED.get(),
                PigEntity::getSaddled,
                PigEntity::setSaddled);

        registerAreaEffectCloudEntityData();
        registerFallingBlockEntityData();
        registerArmorStandEntityData();
        registerMinecartCommandBlockEntityData();
        registerSheepEntityData();
        registerVanishableEntityData();
        registerPlayerEntityData();
        registerWolfEntityData();
        registerCreeperEntityData();
        registerUserData();
        registerHorseEntityData();
        registerAbstractHorseEntityData();
        registerAgeableEntityData();
        registerItemEntityData();
        registerAgentEntityData();
        registerLivingEntityData();
        registerEntityData();
    }

    private void registerCreeperEntityData() {
        register(CreeperEntity.class, Keys.IS_CHARGED.get(),
                CreeperEntity::getPowered,
                (accessor, value) -> accessor.getDataManager().set(CreeperEntityAccessor.accessor$getPowered(), value));
    }

    private void registerFallingBlockEntityData() {
        register(FallingBlockEntityAccessor.class, Keys.BLOCK_STATE.get(),
                (accessor) -> (BlockState) accessor.accessor$getFallTile(),
                (accessor, value) -> accessor.accessor$setFallTile((net.minecraft.block.BlockState) value));

        register(FallingBlockEntityAccessor.class, Keys.CAN_PLACE_AS_BLOCK.get(),
                (accessor) -> !accessor.accessor$getDontSetBlock(),
                (accessor, value) -> accessor.accessor$setDontSetAsBlock(!value));

        register(FallingBlockEntity.class, Keys.SHOULD_DROP.get(),
                (accessor) -> accessor.shouldDropItem,
                (accessor, value) -> accessor.shouldDropItem = value);

        register(FallingBlockEntityAccessor.class, Keys.CAN_HURT_ENTITIES.get(),
                FallingBlockEntityAccessor::accessor$getHurtEntities,
                FallingBlockEntityAccessor::accessor$setHurtEntities);

        register(FallingBlockEntityAccessor.class, Keys.DAMAGE_PER_BLOCK.get(),
                (accessor) -> (double) accessor.accessor$getFallHurtAmount(),
                (accessor, value) -> accessor.accessor$setFallHurtAmount(value.floatValue()));

        register(FallingBlockEntityAccessor.class, Keys.FALL_TIME.get(),
                FallingBlockEntityAccessor::accessor$getFallTime,
                FallingBlockEntityAccessor::accessor$setFallTime);

        register(FallingBlockEntityAccessor.class, Keys.MAX_FALL_DAMAGE.get(),
                (accessor) -> (double) accessor.accessor$getFallHurtMax(),
                (accessor, value) -> accessor.accessor$setFallHurtMax((int) Math.ceil(value)));
    }

    private void registerArmorStandEntityData() {
        register(ArmorStandEntity.class, Keys.ARMOR_STAND_HAS_ARMS.get(),
                ArmorStandEntity::getShowArms,
                (accessor, value) -> ((ArmorStandEntityAccessor) accessor).accessor$setShowArms(value));

        register(ArmorStandEntity.class, Keys.ARMOR_STAND_HAS_BASE_PLATE.get(),
                (accessor) -> !accessor.hasNoBasePlate(),
                (accessor, value) -> ((ArmorStandEntityAccessor) accessor).accessor$setNoBasePlate(!value));

        register(ArmorStandEntity.class, Keys.ARMOR_STAND_HAS_MARKER.get(),
                ArmorStandEntity::hasMarker,
                (accessor, value) -> ((ArmorStandEntityAccessor) accessor).accessor$setMarker(value));

        register(ArmorStandEntity.class, Keys.ARMOR_STAND_IS_SMALL.get(),
                ArmorStandEntity::isSmall,
                (accessor, value) -> ((ArmorStandEntityAccessor) accessor).accessor$setSmall(value));

        register(new ArmorStandEntityRotationProvider<>(Keys.CHEST_ROTATION.get(),
                ArmorStandEntity::getBodyRotation,
                ArmorStandEntity::setBodyRotation));

        register(new ArmorStandEntityRotationProvider<>(Keys.HEAD_ROTATION.get(),
                ArmorStandEntity::getHeadRotation,
                ArmorStandEntity::setHeadRotation));

        register(new ArmorStandEntityRotationProvider<>(Keys.LEFT_ARM_ROTATION.get(),
                ArmorStandEntityAccessor::accessor$getLeftArmRotation,
                ArmorStandEntity::setLeftArmRotation));

        register(new ArmorStandEntityRotationProvider<>(Keys.LEFT_LEG_ROTATION.get(),
                ArmorStandEntityAccessor::accessor$getLeftLegRotation,
                ArmorStandEntity::setLeftLegRotation));

        register(new ArmorStandEntityRotationProvider<>(Keys.RIGHT_ARM_ROTATION.get(),
                ArmorStandEntityAccessor::accessor$getRightArmRotation,
                ArmorStandEntity::setLeftArmRotation));

        register(new ArmorStandEntityRotationProvider<>(Keys.RIGHT_LEG_ROTATION.get(),
                ArmorStandEntityAccessor::accessor$getRightLegRotation,
                ArmorStandEntity::setLeftLegRotation));

        register(new ArmorStandEntityBodyRotationsProvider());
        register(new ArmorStandEntityPlacingDisabledProvider());
        register(new ArmorStandEntityTakingDisabledProvider());
    }

    private void registerMinecartCommandBlockEntityData() {
        register(MinecartCommandBlockEntity.class, Keys.COMMAND.get(),
                (accessor) -> accessor.getCommandBlockLogic().getCommand(),
                (accessor, value) -> ((CommandBlockLogicAccessor) accessor.getCommandBlockLogic()).accessor$setCommandStored(value));

        register(MinecartCommandBlockEntity.class, Keys.SUCCESS_COUNT.get(),
                (accessor) -> accessor.getCommandBlockLogic().getSuccessCount(),
                (accessor, value) -> ((CommandBlockLogicAccessor) accessor.getCommandBlockLogic()).accessor$setSuccessCount(value));

        register(MinecartCommandBlockEntity.class, Keys.TRACKS_OUTPUT.get(),
                (accessor) -> accessor.getCommandBlockLogic().shouldReceiveErrors(),
                (accessor, value) -> accessor.getCommandBlockLogic().setTrackOutput(value));

        register(new MinecartCommandBlockEntityLastCommandOutputProvider());
    }

    private void registerAbstractHorseEntityData() {
        register(new AbstractHorseEntityIsSaddledProvider());
    }

    private void registerHorseEntityData() {
        register(new HorseEntityHorseColorProvider());
        register(new HorseEntityHorseStyleProvider());
    }

    private void registerAreaEffectCloudEntityData() {
        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_AGE.get(),
                (accessor) -> accessor.ticksExisted,
                (accessor, value) -> accessor.ticksExisted = value);

        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_REAPPLICATION_DELAY.get(),
                (accessor) -> accessor.ticksExisted,
                (accessor, value) -> accessor.ticksExisted = value);

        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_COLOR.get(),
                (accessor) -> Color.ofRgb(accessor.getColor()),
                (accessor, value) -> accessor.setColor(value.getRgb()));

        register(AreaEffectCloudEntityAccessor.class, Keys.AREA_EFFECT_CLOUD_DURATION_ON_USE.get(),
                AreaEffectCloudEntityAccessor::accessor$getDurationOnUse,
                AreaEffectCloudEntityAccessor::accessor$setDurationOnUse);

        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_DURATION.get(),
                AreaEffectCloudEntity::getDuration,
                AreaEffectCloudEntity::setDuration);

        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_RADIUS_ON_USE.get(),
                (accessor) -> (double) ((AreaEffectCloudEntityAccessor) accessor).accessor$getRadiusOnUse(),
                (accessor, value) -> accessor.setRadiusOnUse(value.floatValue()));

        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_RADIUS.get(),
                (accessor) -> (double) accessor.getRadius(),
                (accessor, value) -> accessor.setRadius(value.floatValue()));

        register(AreaEffectCloudEntity.class, Keys.AREA_EFFECT_CLOUD_WAIT_TIME.get(),
                (accessor) -> ((AreaEffectCloudEntityAccessor) accessor).accessor$getWaitTime(),
                AreaEffectCloudEntity::setWaitTime);

        register(AreaEffectCloudEntityAccessor.class, Keys.POTION_EFFECTS.get(),
                (accessor) -> PotionEffectHelper.copyAsPotionEffects(accessor.accessor$getEffects()),
                (accessor, value) -> accessor.accessor$setEffects(PotionEffectHelper.copyAsEffectInstances(value)));

        register(new AreaEffectCloudEntityParticleEffectProvider());
    }

    private void registerAgeableEntityData() {
        register(AgeableEntity.class, Keys.AGEABLE_AGE.get(),
                AgeableEntity::getGrowingAge,
                AgeableEntity::setGrowingAge);

        register(AgeableEntity.class, Keys.IS_ADULT.get(),
                (accessor) -> !accessor.isChild(),
                (accessor, value) -> accessor.setGrowingAge(value ? Constants.Entity.Ageable.ADULT : Constants.Entity.Ageable.CHILD));
    }

    private void registerAgentEntityData() {
        register(MobEntityAccessor.class, Keys.IS_AI_ENABLED.get(),
                (accessor) -> !accessor.accessor$isAIDisabled(),
                (accessor, value) -> accessor.accessor$setNoAI(!value));
    }

    private void registerLivingEntityData() {
        register(LivingEntity.class, Keys.ABSORPTION.get(),
                (accessor) -> (double) accessor.getAbsorptionAmount(),
                (accessor, value) -> accessor.setAbsorptionAmount(value.floatValue()));

        register(LivingEntity.class, Keys.STUCK_ARROWS.get(),
                LivingEntity::getArrowCountInEntity,
                (accessor, value) -> accessor.setArrowCountInEntity(MathHelper.clamp(value, 0, Integer.MAX_VALUE)));

        register(new LivingEntityActiveItemProvider());
        register(new LivingEntityBodyRotationsProvider());
        register(new LivingEntityChestRotationProvider());
        register(new LivingEntityHeadRotationProvider());
        register(new LivingEntityHealthProvider());
        register(new LivingEntityLastAttackerProvider());
        register(new LivingEntityMaxAirProvider());
        register(new LivingEntityMaxHealthProvider());
        register(new LivingEntityPotionEffectsProvider());
        register(new LivingEntityRemainingAirProvider());
    }

    private void registerEntityData() {
        register(Entity.class, Keys.VELOCITY.get(),
                (accessor) -> VecHelper.toVector3d(accessor.getMotion()),
                (accessor, value) -> accessor.setMotion(VecHelper.toVec3d(value)));

        register(Entity.class, Keys.IS_WET.get(), Entity::isWet);
        register(Entity.class, Keys.IS_SNEAKING.get(), Entity::isSneaking, Entity::setSneaking);
        register(Entity.class, Keys.IS_SPRINTING.get(), Entity::isSprinting, Entity::setSprinting);
        register(Entity.class, Keys.ON_GROUND.get(), entity -> entity.onGround);

        register(new EntityDisplayNameProvider());
        register(new EntityFireDamageDelayProvider());
        register(new EntityInvisibleProvider());
        register(new EntityInvulnerabilityTicksProvider());
    }

    private void registerSheepEntityData() {
        register(SheepEntity.class, Keys.DYE_COLOR.get(),
                (accessor) -> (DyeColor) (Object) accessor.getFleeceColor(),
                (accessor, value) -> accessor.setFleeceColor((net.minecraft.item.DyeColor) (Object) value));

        register(SheepEntity.class, Keys.IS_SHEARED.get(),
                SheepEntity::getSheared,
                SheepEntity::setSheared);
    }

    private void registerWolfEntityData() {
        register(WolfEntity.class, Keys.DYE_COLOR.get(),
                (accessor) -> (DyeColor) (Object) accessor.getCollarColor(),
                (accessor, value) -> accessor.setCollarColor((net.minecraft.item.DyeColor) (Object) value));

        register(new WolfEntityIsWetProvider());
    }

    private void registerVanishableEntityData() {
        register(new VanishableEntityVanishProvider());
        register(new VanishableEntityVanishIgnoresCollisionProvider());
        register(new VanishableEntityVanishPreventsTargetingProvider());
    }

    private void registerPlayerEntityData() {
        register(PlayerEntity.class, Keys.DOMINANT_HAND.get(),
                (accessor) -> (HandPreference) (Object) accessor.getPrimaryHand(),
                (accessor, value) -> accessor.setPrimaryHand((HandSide) (Object) value));

        register(new PlayerEntityExhaustionProvider());
        register(new PlayerEntityFlyingSpeedProvider());
        register(new PlayerEntityFoodLevelProvider());
        register(new PlayerEntitySaturationProvider());
        register(new PlayerEntityWalkingSpeedProvider());
    }

    private void registerItemEntityData() {
        register(ItemEntityBridge.class, Keys.DESPAWN_DELAY.get(),
                ItemEntityBridge::bridge$getDespawnDelay,
                ItemEntityBridge::bridge$setDespawnDelay);

        register(ItemEntity.class, Keys.PICKUP_DELAY.get(),
                (accessor) -> ((ItemEntityBridge) accessor).bridge$getPickupDelay(),
                ItemEntity::setPickupDelay);
    }

    private void registerUserData() {
        register(new UserFirstDatePlayedProvider());
        register(new UserLastDatePlayedProvider());
    }
}
