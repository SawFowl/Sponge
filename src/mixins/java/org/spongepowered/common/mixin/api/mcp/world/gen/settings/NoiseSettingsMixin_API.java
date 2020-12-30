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
package org.spongepowered.common.mixin.api.mcp.world.gen.settings;

import net.minecraft.world.gen.settings.NoiseSettings;
import net.minecraft.world.gen.settings.ScalingSettings;
import org.spongepowered.api.world.generation.settings.noise.SamplingSettings;
import org.spongepowered.api.world.generation.settings.noise.SlideSettings;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseSettings.class)
@Implements(@Interface(iface = org.spongepowered.api.world.generation.settings.noise.NoiseSettings.class, prefix = "noiseSettings$"))
public abstract class NoiseSettingsMixin_API implements org.spongepowered.api.world.generation.settings.noise.NoiseSettings {

    // @formatter:off
    @Shadow public abstract int shadow$height();
    @Shadow public abstract ScalingSettings shadow$noiseSamplingSettings();
    @Shadow public abstract net.minecraft.world.gen.settings.SlideSettings shadow$topSlideSettings();
    @Shadow public abstract net.minecraft.world.gen.settings.SlideSettings shadow$bottomSlideSettings();
    @Shadow public abstract int shadow$noiseSizeHorizontal();
    @Shadow public abstract int shadow$noiseSizeVertical();
    @Shadow public abstract double shadow$densityFactor();
    @Shadow public abstract double shadow$densityOffset();
    @Shadow @Deprecated public abstract boolean shadow$useSimplexSurfaceNoise();
    @Shadow @Deprecated public abstract boolean shadow$randomDensityOffset();
    @Shadow @Deprecated public abstract boolean shadow$isAmplified();
    // @formatter:on

    @Intrinsic
    public int noiseSettings$height() {
        return this.shadow$height();
    }

    @Override
    public SamplingSettings samplingSettings() {
        return (SamplingSettings) this.shadow$noiseSamplingSettings();
    }

    @Override
    public SlideSettings topSettings() {
        return (SlideSettings) this.shadow$topSlideSettings();
    }

    @Override
    public SlideSettings bottomSettings() {
        return (SlideSettings) this.shadow$bottomSlideSettings();
    }

    @Override
    public int horizontalSize() {
        return this.shadow$noiseSizeHorizontal();
    }

    @Override
    public int verticalSize() {
        return this.shadow$noiseSizeVertical();
    }

    @Intrinsic
    public double noiseSettings$densityFactor() {
        return this.shadow$densityFactor();
    }

    @Intrinsic
    public double noiseSettings$densityOffset() {
        return this.shadow$densityOffset();
    }

    @Override
    public boolean simplexForSurface() {
        return this.shadow$useSimplexSurfaceNoise();
    }

    @Override
    public boolean randomizeDensityOffset() {
        return this.shadow$randomDensityOffset();
    }

    @Override
    public boolean amplified() {
        return this.shadow$isAmplified();
    }
}