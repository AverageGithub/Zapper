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
package revxrsal.zapper.repository;

import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.Dependency;

import java.net.URL;
import java.util.Objects;

/**
 * Represents a Maven repository with a URL
 */
public final class MavenRepository implements Repository {

    private static final MavenRepository MAVEN_CENTRAL = new MavenRepository("https://repo1.maven.org/maven2/");
    private static final MavenRepository JITPACK = new MavenRepository("https://jitpack.io/");
    private static final MavenRepository MINECRAFT = new MavenRepository("https://libraries.minecraft.net/");
    private static final MavenRepository PAPER = new MavenRepository("https://papermc.io/repo/repository/maven-public/");

    public static @NotNull MavenRepository mavenCentral() {
        return MAVEN_CENTRAL;
    }

    public static @NotNull MavenRepository jitpack() {
        return JITPACK;
    }

    public static @NotNull MavenRepository minecraft() {
        return MINECRAFT;
    }

    public static @NotNull MavenRepository paper() {
        return PAPER;
    }

    public static @NotNull MavenRepository maven(@NotNull String url) {
        return new MavenRepository(url);
    }

    private final String repoURL;

    private MavenRepository(@NotNull String repoURL) {
        if (repoURL.charAt(repoURL.length() - 1) != '/')
            repoURL += '/';
        this.repoURL = repoURL;
    }

    public String getRepositoryURL() {
        return repoURL;
    }

    @Override
    public String toString() {
        return getRepositoryURL();
    }

    public @NotNull URL resolve(@NotNull Dependency dependency) throws Exception {
        return new URL(repoURL + dependency.getMavenPath());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MavenRepository that = (MavenRepository) o;
        return Objects.equals(repoURL, that.repoURL);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(repoURL);
    }
}
