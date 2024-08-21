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
package org.spongepowered.forge.applaunch.loading.moddiscovery;

import com.google.common.collect.ImmutableMap;
import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.Launcher;

import net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.spongepowered.forge.applaunch.plugin.ForgePluginPlatform;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpongeForgeModLocator extends FMLJavaModLanguageProvider implements IModFileCandidateLocator {
    private static final Logger LOGGER = LogManager.getLogger();
    private List<IModFile> modFiles = new ArrayList<>();

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        try {
            URL rootJar = SpongeForgeModLocator.class.getProtectionDomain().getCodeSource().getLocation();
            FileSystem fs =  FileSystems.getFileSystem(rootJar.toURI()); // FML has already opened a file system for this jar
            modFiles = Files.list(fs.getPath("jars"))
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                .map(path -> {
                    try {
                        URI jij = new URI("jij:" + path.toAbsolutePath().toUri().getRawSchemeSpecificPart()).normalize();
                        final Map<String, ?> env = ImmutableMap.of("packagePath", path);
                        FileSystem jijFS = FileSystems.newFileSystem(jij, env);
                        return jijFS.getPath("/"); // root of the archive to load
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(path -> path != null).map(this::createMod).toList();
        } catch (Exception e) {
            LOGGER.error("Failed to scan mod candidates", e);
        }
    }

    private IModFile createMod(Path path) {
        return ModFileParsers.newPluginInstance(this, path);
    }

    @Override
    public String name() {
        return "spongeneoforge";
    }

    //@Override
    public void initArguments(Map<String, ?> arguments) {
        final Environment env = Launcher.INSTANCE.environment();
        ForgePluginPlatform.bootstrap(env);
    }
}
