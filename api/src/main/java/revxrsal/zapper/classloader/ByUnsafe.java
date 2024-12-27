/*
 * This file is part of Zapper, licensed under the MIT License.
 *
 *  Copyright (c) Revxrsal <reflxction.github@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package revxrsal.zapper.classloader;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import static revxrsal.zapper.classloader.UnsafeUtil.getField;
import static revxrsal.zapper.classloader.UnsafeUtil.isJava8;

/**
 * An implementation that uses sun.misc.Unsafe to inject URLs
 */
final class ByUnsafe extends URLClassLoaderWrapper {
    private static final Class<? extends URLClassLoader> PL_CL_LOADER;
    private static BiFunction<URL[], ClassLoader, URLClassLoader> LOADER_FACTORY;
    private static Class<?> LIBRARY_LOADER;
    private final List<URL> urls;
    private final URLClassLoader loader;
    private final Collection<URL> unopenedURLs;
    private final List<URL> pathURLs;

    public ByUnsafe(@NotNull URLClassLoader loader) {
        this.loader = loader;
        List<URL> urls0 = null;
        if (PL_CL_LOADER.isAssignableFrom(loader.getClass())) {
            try {
                ClassLoader libraryLoader = UnsafeUtil.getFieldNullable(loader, "libraryLoader", PL_CL_LOADER);
                urls0 = new ArrayList<>();
            } catch (Exception exception) {

            }
        }
        urls = urls0;

        Object ucp = getField(loader, "ucp", URLClassLoader.class);
        unopenedURLs = getField(ucp, isJava8() ? "urls" : "unopenedUrls", ucp.getClass());
        pathURLs = getField(ucp, "path", ucp.getClass());
    }

    public void addURL(@NotNull URL url) {
        if (urls != null && LIBRARY_LOADER != null && LOADER_FACTORY != null) {
            urls.add(url);
            return;
        }

        unopenedURLs.add(url);
        pathURLs.add(url);
    }

    @Override
    public void flush() {
        if (urls != null && !urls.isEmpty()) {
            List<URL> urlsCopy = new ArrayList<>(urls);
            ClassLoader prevLoader = UnsafeUtil.getFieldNullable(loader, "libraryLoader", PL_CL_LOADER);
            if (prevLoader instanceof URLClassLoader) {
                URL[] prevUrls = ((URLClassLoader) prevLoader).getURLs();
                for (URL url : prevUrls) {
                    if (urlsCopy.contains(url)) {
                        continue;
                    }
                    urlsCopy.add(url);
                }
            }
            URLClassLoader classLoader;
            if (LOADER_FACTORY == null) {
                classLoader = new URLClassLoader(urlsCopy.toArray(new URL[0]), LIBRARY_LOADER.getClassLoader());
            } else {
                classLoader = LOADER_FACTORY.apply(urlsCopy.toArray(new URL[0]), LIBRARY_LOADER.getClassLoader());
            }

            UnsafeUtil.setField(loader, "libraryLoader", PL_CL_LOADER, classLoader);
        }
    }

    static {
        try {
            PL_CL_LOADER = Class.forName("org.bukkit.plugin.java.PluginClassLoader")
                    .asSubclass(URLClassLoader.class);
            try {
                LIBRARY_LOADER = Class.forName("org.bukkit.plugin.java.LibraryLoader");
            } catch (Exception e) {
                LIBRARY_LOADER = null;
            }
            try {
                LOADER_FACTORY = UnsafeUtil.getFieldReflection(null, "LIBRARY_LOADER_FACTORY", LIBRARY_LOADER);
            } catch (Exception e) {
                LOADER_FACTORY = null;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}