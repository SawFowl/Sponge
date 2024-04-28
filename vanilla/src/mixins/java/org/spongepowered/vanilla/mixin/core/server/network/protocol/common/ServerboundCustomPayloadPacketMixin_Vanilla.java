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
package org.spongepowered.vanilla.mixin.core.server.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class ServerboundCustomPayloadPacketMixin_Vanilla {

    // @formatter: off
    @Shadow @Final private static int MAX_PAYLOAD_SIZE;
    // @formatter: on

    @Inject(method = "readPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/ServerboundCustomPayloadPacket;readUnknownPayload(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/network/protocol/common/custom/DiscardedPayload;"), cancellable = true)
    private static void impl$onReadUnknownPayload(ResourceLocation id, FriendlyByteBuf buf, final CallbackInfoReturnable<CustomPacketPayload> cir) {
        int readableBytes = buf.readableBytes();
        if (readableBytes >= 0 && readableBytes <= ServerboundCustomPayloadPacketMixin_Vanilla.MAX_PAYLOAD_SIZE) {
            final var payload = new FriendlyByteBuf(buf.readBytes(readableBytes));

            cir.setReturnValue(new CustomPacketPayload() {
                @Override
                public void write(FriendlyByteBuf buf) {
                    buf.writeBytes(payload.copy());
                }

                @Override
                public ResourceLocation id() {
                    return id;
                }
            });
        } else {
            throw new IllegalArgumentException("Payload may not be larger than " + ServerboundCustomPayloadPacketMixin_Vanilla.MAX_PAYLOAD_SIZE + " bytes");
        }
    }
}