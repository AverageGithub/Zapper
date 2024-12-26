package revxrsal.zapper.remapper;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class PaperLibraryRemapper {
    private static final Function<List<Path>, List<Path>> remapperFunction;

    static {
        Function<List<Path>, List<Path>> remapperFunction1;
        try {
            Class<?> LIBRARY_LOADER_CLASS = Class.forName("org.bukkit.plugin.java.LibraryLoader");
            remapperFunction1 = (Function<List<Path>, List<Path>>) LIBRARY_LOADER_CLASS.getDeclaredField("REMAPPER").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            remapperFunction1 = Function.identity();
        }

        remapperFunction = remapperFunction1;
    }

    public static File tryRemap(File file) {
        return remapperFunction.apply(Collections.singletonList(file.toPath())).stream()
                .findFirst()
                .orElseThrow(NullPointerException::new)
                .toFile();
    }
}
