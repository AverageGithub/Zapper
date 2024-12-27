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
package revxrsal.zapper;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.zapper.repository.Repository;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Represents a runtime dependency. Note that this does not include transitive
 * dependencies
 */
public final class Dependency {

    private static final String MAVEN_PATH = "%s/%s/%s/%s-%s%s.jar";

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String mavenPath;

    public Dependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        this(groupId, artifactId, version, null);
    }

    public Dependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, @Nullable String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        mavenPath = String.format(MAVEN_PATH,
                this.groupId.replace('.', '/'),
                this.artifactId,
                this.version,
                this.artifactId,
                this.version,
                classifier == null || StringUtils.isBlank(classifier) ? "" : '-' + classifier
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency)) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @CheckReturnValue
    public @NotNull DependencyDownloadResult download(@NotNull File file, @NotNull Repository repository) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            URL url = repository.resolve(this);
            try (InputStream depIn = url.openStream()) {
                try (OutputStream outStream = Files.newOutputStream(file.toPath())) {
                    byte[] buffer = new byte[8 * 1024];
                    int bytesRead;
                    while ((bytesRead = depIn.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            return DependencyDownloadResult.success();
        } catch (Throwable t) {
            file.delete();
            return DependencyDownloadResult.failure(t);
        }
    }

    public String getClassifier() {
        return this.classifier;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getVersion() {
        return this.version;
    }

    public String getMavenPath() {
        return this.mavenPath;
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                '}';
    }
}