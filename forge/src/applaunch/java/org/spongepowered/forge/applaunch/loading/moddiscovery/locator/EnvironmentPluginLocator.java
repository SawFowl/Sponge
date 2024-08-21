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
package org.spongepowered.forge.applaunch.loading.moddiscovery.locator;

import net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;

import org.spongepowered.forge.applaunch.loading.moddiscovery.ModFileParsers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class EnvironmentPluginLocator extends FMLJavaModLanguageProvider implements IModFileCandidateLocator {

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        for(Path[] arrayPaths : getPluginsPaths()) for(Path path : arrayPaths) context.addLocated(path);
        scanMods().forEach(scan -> {
            scan.getIModInfoData().forEach(file -> {
                pipeline.addModFile(file.getFile());
                context.addLocated(file.getFile().getFilePath());
            });
        });
    }

    //@Override
    public List<ModFileScanData> scanMods() {
        final List<ModFileScanData> modFiles = new ArrayList<>();
        for (final Path[] paths : getPluginsPaths()) {
        	ModFileScanData data = new ModFileScanData();
        	data.addModFileInfo(ModFileParsers.newPluginInstance(this, paths).getModFileInfo());
        }
        return modFiles;
    }

    //@Override
    protected ModFileScanData createMod(Path path) {
    	ModFileScanData data = new ModFileScanData();
    	data.addModFileInfo(ModFileParsers.newPluginInstance(this, path).getModFileInfo());
        return data;
    }

    @Override
    public String name() {
        return "environment plugin";
    }

    //@Override
    public void initArguments(final Map<String, ?> arguments) {
    }

    private static List<Path[]> getPluginsPaths() {
        final String env = System.getenv("SPONGE_PLUGINS");
        if (env == null) {
            return Collections.emptyList();
        }

        List<Path[]> plugins = new ArrayList<>();
        for (final String entry : env.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            plugins.add(Stream.of(entry.split("&")).map(Path::of).toArray(Path[]::new));
        }

        return plugins;
    }
}
