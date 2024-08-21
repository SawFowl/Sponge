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
package org.spongepowered.forge.launch.event;

import net.neoforged.bus.*;
import net.neoforged.bus.api.*;

import java.lang.reflect.Field;
import java.util.Objects;

public final class SpongeEventBus extends EventBus {

    // I hope ya'll like reflection...
    private static Field busIDField;
    private static Field exceptionHandlerField;
    private static Field trackPhasesField;
    private static Field shutdownField;
    private static Field baseTypeField;
    private static Field checkTypesOnDispatchField;

    static {
        try {
            SpongeEventBus.busIDField = EventBus.class.getDeclaredField("busID");
            SpongeEventBus.exceptionHandlerField = EventBus.class.getDeclaredField("exceptionHandler");
            SpongeEventBus.trackPhasesField = EventBus.class.getDeclaredField("trackPhases");
            SpongeEventBus.shutdownField = EventBus.class.getDeclaredField("shutdown");
            SpongeEventBus.baseTypeField = EventBus.class.getDeclaredField("baseType");
            SpongeEventBus.checkTypesOnDispatchField = EventBus.class.getDeclaredField("checkTypesOnDispatch");

        } catch (final Exception ex) {
            // Burn this to the ground
            throw new RuntimeException(ex);
        }
    }

    // reflected fields that are stored again to prevent multiple reflection calls
    private final int rbusID;
    private final IEventExceptionHandler rexceptionHandler;
    private final boolean rtrackPhases;
    private boolean rshutdown;
    private final Class<?> rbaseType;
    private final boolean rcheckTypesOnDispatch;

    public SpongeEventBus(final BusBuilderImpl busBuilder) {
        super(busBuilder);

        // Sponge Start - I hope ya'll still like reflection
        try {
            SpongeEventBus.busIDField.setAccessible(true);
            SpongeEventBus.exceptionHandlerField.setAccessible(true);
            SpongeEventBus.trackPhasesField.setAccessible(true);
            SpongeEventBus.shutdownField.setAccessible(true);
            SpongeEventBus.baseTypeField.setAccessible(true);
            SpongeEventBus.checkTypesOnDispatchField.setAccessible(true);

            this.rbusID = SpongeEventBus.busIDField.getInt(this);
            this.rexceptionHandler = (IEventExceptionHandler) SpongeEventBus.exceptionHandlerField.get(this);
            this.rtrackPhases = SpongeEventBus.trackPhasesField.getBoolean(this);
            this.rshutdown = SpongeEventBus.shutdownField.getBoolean(this);
            this.rbaseType = (Class<?>) SpongeEventBus.baseTypeField.get(this);
            this.rcheckTypesOnDispatch = SpongeEventBus.checkTypesOnDispatchField.getBoolean(this);

            SpongeEventBus.busIDField.setAccessible(false);
            SpongeEventBus.exceptionHandlerField.setAccessible(false);
            SpongeEventBus.trackPhasesField.setAccessible(false);
            SpongeEventBus.shutdownField.setAccessible(false);
            SpongeEventBus.baseTypeField.setAccessible(false);
            SpongeEventBus.checkTypesOnDispatchField.setAccessible(false);
        } catch (final Exception ex) {
            // Burn this to the ground again
            throw new RuntimeException(ex);
        }
        // Sponge End
    }

    @Override
    public void start() {
        super.start();
        this.rshutdown = false;
    }
}
