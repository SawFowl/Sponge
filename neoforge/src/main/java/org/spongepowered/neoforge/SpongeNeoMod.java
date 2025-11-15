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
package org.spongepowered.neoforge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Client;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.channel.Channel;
import org.spongepowered.common.entity.SpongeEntityTypes;
import org.spongepowered.common.entity.living.human.HumanEntity;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.launch.Launch;
import org.spongepowered.common.launch.Lifecycle;
import org.spongepowered.common.launch.config.core.ConfigHandle;
import org.spongepowered.common.network.channel.SpongeChannel;
import org.spongepowered.common.network.channel.SpongeChannelManager;
import org.spongepowered.common.network.channel.SpongeChannelPayload;
import org.spongepowered.common.network.packet.SpongePacketHandler;
import org.spongepowered.neoforge.hook.NeoChannelHooks;
import org.spongepowered.neoforge.hook.NeoEventHooks;
import org.spongepowered.neoforge.hook.NeoGeneralHooks;
import org.spongepowered.neoforge.hook.NeoItemHooks;
import org.spongepowered.neoforge.hook.NeoWorldHooks;

import java.util.Map;

@Mod("spongeneo")
public final class SpongeNeoMod {

    private final Logger logger = LogManager.getLogger("spongeneo");
    private static Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> neoPayloadRegistrations;

    public SpongeNeoMod(IEventBus modBus) {
        // modBus: add all FML events with it
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onEntityAttributeCreationEvent);

        // For this event listener to work correctly, it must register channels after mods.
        // But I think it's acceptable not to make it the latest.
        modBus.addListener(EventPriority.LOW, this::onRegisterPayloadHandlersEvent);

        // annotation events, for non-FML things
        NeoForge.EVENT_BUS.register(this);

        // Set platform hooks as required
        PlatformHooks.INSTANCE.setEventHooks(new NeoEventHooks());
        PlatformHooks.INSTANCE.setWorldHooks(new NeoWorldHooks());
        PlatformHooks.INSTANCE.setGeneralHooks(new NeoGeneralHooks());
        PlatformHooks.INSTANCE.setChannelHooks(new NeoChannelHooks());
        PlatformHooks.INSTANCE.setItemHooks(new NeoItemHooks());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.callConstructEvent();
        lifecycle.callRegisterFactoryEvent();
        lifecycle.callRegisterBuilderEvent();
        lifecycle.callRegisterChannelEvent();
        lifecycle.establishGameServices();
        lifecycle.establishDataKeyListeners();

        SpongePacketHandler.init((SpongeChannelManager) Sponge.channelManager());

        this.logger.info("SpongeNeo v{} initialized", Launch.instance().platformPlugin().metadata().version());
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        final Client minecraft = (Client) Minecraft.getInstance();
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.establishDataProviders();
        lifecycle.callRegisterDataEvent();
        lifecycle.establishClientRegistries(minecraft);
        lifecycle.callStartingEngineEvent(minecraft);
    }

    @SubscribeEvent
    public void onServerAboutToStart(final ServerAboutToStartEvent event) {
        // Save config now that registries have been initialized
        ConfigHandle.setSaveSuppressed(false);

        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.establishServerServices();

        lifecycle.establishServerFeatures();

        lifecycle.establishServerRegistries((Server) event.getServer());
        lifecycle.callStartingEngineEvent((Server) event.getServer());
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.callStartedEngineEvent((Server) event.getServer());

        lifecycle.callLoadedGameEvent();
    }

    @SubscribeEvent
    public void onServerStoppingEvent(final ServerStoppingEvent event) {
        final Lifecycle lifecycle = Launch.instance().lifecycle();
        lifecycle.callStoppingEngineEvent((Server) event.getServer());
    }

    public void onEntityAttributeCreationEvent(final EntityAttributeCreationEvent event) {
        event.put(SpongeEntityTypes.HUMAN, HumanEntity.createAttributes());
    }

    public void onRegisterPayloadHandlersEvent(final RegisterPayloadHandlersEvent event) {
        for(Channel channel : ((SpongeChannelManager) Sponge.channelManager()).channels()) {
            if(channel.key().namespace().equals("minecraft")) continue;
            var existingHandler = neoPayloadRegistrations.get(ConnectionProtocol.PLAY).get((ResourceLocation) (Object) channel.key());
            if(existingHandler == null) {
                event.registrar("1")
                    .optional()
                    .playBidirectional(
                        ((SpongeChannel) channel).payloadType(),
                        SpongeChannelPayload.streamCodec(((SpongeChannel) channel).payloadType(), 32767),
                        (payload, context) -> {
                            if(!(context.player() instanceof Player player)) return;
                            // The initial packet listening processing should be moved here.
                            // I do not know the full implementation of Sponge's package management, so I cannot do it on my own.
                            // This part of the code does not require obtaining and using a codec registered via the NeoForge API.

                        }
                    );
            } else neoPayloadRegistrations.get(ConnectionProtocol.PLAY).put((ResourceLocation) (Object) channel.key(), createNewHandler(existingHandler));
        }
    }

    public static void setPayloadRegistrations(Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> map) {
        if(neoPayloadRegistrations == null) neoPayloadRegistrations = map;
    }

    private PayloadRegistration<?> createNewHandler(PayloadRegistration existingHandler) {
        return new PayloadRegistration(existingHandler.type(), existingHandler.codec(), (payload, context) -> {
            // Since the data channel has already been registered by another mod, then in this place you need to use the codec that was previously registered in NeoForge.
            // This avoids the ClassCastException error.
            // Since Sponge can be installed on the client, obtaining the necessary codec may need to be improved.
            var codec = (StreamCodec<ByteBuf, CustomPacketPayload>) NetworkRegistry.getCodec(payload.type().id(), ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            codec.encode(buffer, payload);
            if(context.player() instanceof Player player && buffer.hasArray()) {
                // The initial packet listening processing should be moved here.
                // I do not know the full implementation of Sponge's package management, so I cannot do it on my own.

            }
            ((IPayloadHandler)existingHandler.handler()).handle(payload, context);
        }, existingHandler.protocols(), existingHandler.flow(), existingHandler.version(), existingHandler.optional());
    }

}
