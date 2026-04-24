/*
 *     Copyright (C) 2026 Wuason6x9 and RubenArtz
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.wuason.toastapi.nms;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class NmsModuleLoader {

    private static final Map<String, LoadedModule> LOADED_MODULES = new ConcurrentHashMap<>();

    private NmsModuleLoader() {
    }

    @NotNull
    public static IToastWrapper load(@NotNull String moduleName,
                                     @NotNull String implementationClassName) {
        Objects.requireNonNull(moduleName);
        Objects.requireNonNull(implementationClassName);

        try {
            LoadedModule loadedModule = LOADED_MODULES.computeIfAbsent(moduleName, key -> {
                try {
                    return loadModule(key);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            Class<?> implementationClass = Class.forName(
                    implementationClassName,
                    true,
                    loadedModule.classLoader()
            );

            Constructor<?> constructor = implementationClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (IToastWrapper) constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load NMS wrapper from module " + moduleName, exception);
        }
    }

    private static LoadedModule loadModule(@NotNull String moduleName) throws IOException {
        String resourcePath = "/nms_modules/" + moduleName + ".jar";

        File pluginsDir = new File("plugins");
        File artzLibsDir = new File(pluginsDir, "Artz-Libraries");
        File moduleDirectory = new File(artzLibsDir, "SimpleToastApi-NMS");

        if (!moduleDirectory.exists()) {
            moduleDirectory.mkdirs();
        }

        File moduleFile = new File(moduleDirectory, moduleName + ".jar");

        try (InputStream inputStream = NmsModuleLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(inputStream, moduleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{moduleFile.toURI().toURL()},
                NmsModuleLoader.class.getClassLoader()
        );

        return new LoadedModule(moduleFile.toPath(), classLoader);
    }

    private record LoadedModule(Path path, URLClassLoader classLoader) {
    }
}