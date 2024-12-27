// Class copied from libby, licensed under MIT license
package revxrsal.zapper;

import revxrsal.zapper.classloader.IsolatedClassLoader;
import revxrsal.zapper.repository.MavenRepository;
import revxrsal.zapper.repository.Repository;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TransitiveDependencyHelper {

    /**
     * com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector class name for reflections
     */
    private static final String TRANSITIVE_DEPENDENCY_COLLECTOR_CLASS = replaceWithDots("com{}alessiodp{}libby{}maven{}resolver{}TransitiveDependencyCollector");

    /**
     * org.eclipse.aether.artifact.Artifact class name for reflections
     */
    private static final String ARTIFACT_CLASS = replaceWithDots("org{}eclipse{}aether{}artifact{}Artifact");

    private final Object transitiveDependencyCollectorObject;

    /**
     * Reflected method for resolving transitive dependencies
     */
    private final Method resolveTransitiveDependenciesMethod;

    /**
     * Reflected getter methods of Artifact class
     */
    private final Method artifactGetGroupIdMethod, artifactGetArtifactIdMethod, artifactGetVersionMethod, artifactGetBaseVersionMethod, artifactGetClassifierMethod;

    private final DependencyManager libraryManager;

    /**
     * Creates a new transitive dependency helper using the provided library manager to
     * download the dependencies required for transitive dependency resolution in runtime.
     *
     * @param libraryManager the library manager used to download dependencies
     * @param saveDirectory  the directory where all transitive dependencies would be saved
     */
    public TransitiveDependencyHelper(DependencyManager libraryManager, Path saveDirectory) {
        this.libraryManager = libraryManager;

        URL[] urls = new URL[1];

        Dependency d = new Dependency(replaceWithDots("com{}alessiodp{}libby{}maven{}resolver"), "libby-maven-resolver", "1.0.1");
        File file = new File(saveDirectory.toFile(), String.format("%s.%s-%s.jar", d.getGroupId(), d.getArtifactId(), d.getVersion()));
        if (!file.exists()) {
            d.download(file, Repository.maven("https://repo.alessiodp.com/releases/"));
        }
        try {
            urls[0] = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        IsolatedClassLoader classLoader = new IsolatedClassLoader(urls);

        try {
            Class<?> transitiveDependencyCollectorClass = classLoader.loadClass(TRANSITIVE_DEPENDENCY_COLLECTOR_CLASS);
            Class<?> artifactClass = classLoader.loadClass(ARTIFACT_CLASS);

            // com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector(Path)
            Constructor<?> constructor = transitiveDependencyCollectorClass.getConstructor(Path.class);
            constructor.setAccessible(true);
            transitiveDependencyCollectorObject = constructor.newInstance(saveDirectory);
            // com.alessiodp.libby.maven.resolver.TransitiveDependencyCollector#findTransitiveDependencies(String, String, String, String, Stream<String>)
            resolveTransitiveDependenciesMethod = transitiveDependencyCollectorClass.getMethod("findTransitiveDependencies", String.class, String.class, String.class, String.class, Stream.class);
            resolveTransitiveDependenciesMethod.setAccessible(true);
            // org.eclipse.aether.artifact.Artifact#getGroupId()
            artifactGetGroupIdMethod = artifactClass.getMethod("getGroupId");
            // org.eclipse.aether.artifact.Artifact#getArtifactId()
            artifactGetArtifactIdMethod = artifactClass.getMethod("getArtifactId");
            // org.eclipse.aether.artifact.Artifact#getVersion()
            artifactGetVersionMethod = artifactClass.getMethod("getVersion");
            // org.eclipse.aether.artifact.Artifact#getBaseVersion()
            artifactGetBaseVersionMethod = artifactClass.getMethod("getBaseVersion");
            // org.eclipse.aether.artifact.Artifact#getClassifier()
            artifactGetClassifierMethod = artifactClass.getMethod("getClassifier");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<Dependency> findTransitiveLibraries(Dependency library) {
        List<Dependency> transitiveLibraries = new ArrayList<>();

        Collection<Repository> globalRepositories = libraryManager.repositories();
        if (globalRepositories.isEmpty()) {
            throw new IllegalArgumentException("No repositories have been added before resolving transitive dependencies");
        }

        Stream<String> repositories = globalRepositories.stream().map(r -> {
            return ((MavenRepository) r).getRepositoryURL();
        });
        try {
            Collection<?> resolvedArtifacts = (Collection<?>) resolveTransitiveDependenciesMethod.invoke(transitiveDependencyCollectorObject,
                    library.getGroupId(),
                    library.getArtifactId(),
                    library.getVersion(),
                    library.getClassifier(),
                    repositories);
            for (Object resolved : resolvedArtifacts) {
                Map.Entry<?, ?> resolvedEntry = (Map.Entry<?, ?>) resolved;
                Object artifact = resolvedEntry.getKey();
                String repository = (String) resolvedEntry.getValue();

                String groupId = (String) artifactGetGroupIdMethod.invoke(artifact);
                String artifactId = (String) artifactGetArtifactIdMethod.invoke(artifact);
                String baseVersion = (String) artifactGetBaseVersionMethod.invoke(artifact);
                String classifier = (String) artifactGetClassifierMethod.invoke(artifact);

                if (library.getGroupId().equals(groupId) && library.getArtifactId().equals(artifactId))
                    continue;


                Dependency dependency = new Dependency(groupId, artifactId, baseVersion, classifier);
//                if (repository != null) {
//                    // Construct direct download URL
//
//                    // Add ending "/" if missing
//                    if (!repository.endsWith("/")) {
//                        repository = repository + '/';
//                    }
//
//                    // TODO Uncomment the line below once LibraryManager#resolveLibrary stops resolving snapshots
//                    //      for every repository before trying direct URLs
//                    // Make sure the repository is added as fallback if the dependency isn't found at the constructed URL
//                    // libraryBuilder.fallbackRepository(repository);
//
//                    // For snapshots, getVersion() returns version-timestamp-buildNumber instead of version-SNAPSHOT
//                    String version = (String) artifactGetVersionMethod.invoke(artifact);
//
//                    String partialPath = Util.craftPartialPath(artifactId, groupId, baseVersion);
//                    String path = Util.craftPath(partialPath, artifactId, version, classifier);
//
//                    libraryManager.repository(repository + path);
//                } else {
//                    library.getRepositories().forEach(libraryBuilder::repository);
//                    library.getFallbackRepositories().forEach(libraryBuilder::fallbackRepository);
//                }

                transitiveLibraries.add(dependency);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return Collections.unmodifiableCollection(transitiveLibraries);
    }

    private static String replaceWithDots(String string) {
        return string.replace("{}", ".");
    }
}
