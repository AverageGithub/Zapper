package revxrsal.zapper.remapper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class PaperLibraryRemapper {
    private static final Function<Path, Path> remapperFunction;

    static {
        Function<Path, Path> remapperFunction1;
        try {
            Class<?> PLUGIN_INITIALIZE_MANAGER = Class.forName("io.papermc.paper.plugin.PluginInitializerManager");
            Field IMPL_FIELD = PLUGIN_INITIALIZE_MANAGER.getDeclaredField("impl");
            IMPL_FIELD.setAccessible(true);
            Object PLUGIN_INITIALIZE_MANAGER_INSTANCE = IMPL_FIELD.get(null);
            Field REMAPPER_FIELD = PLUGIN_INITIALIZE_MANAGER.getDeclaredField("pluginRemapper");
            Object REMAPPER_INSTANCE = REMAPPER_FIELD.get(PLUGIN_INITIALIZE_MANAGER_INSTANCE);
            if (REMAPPER_INSTANCE == null) {
                remapperFunction1 = Function.identity();
            } else {
                Method REWRITE_METHOD = REMAPPER_INSTANCE.getClass().getDeclaredMethod("rewritePlugin", Path.class);
                remapperFunction1 = path -> {
                    try {
                        return (Path) REWRITE_METHOD.invoke(REMAPPER_INSTANCE, path);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                };
            }
//            remapperFunction1 = (Function<List<Path>, List<Path>    >) LIBRARY_LOADER_CLASS.getDeclaredField("REMAPPER").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException exception) {
            remapperFunction1 = Function.identity();
            exception.printStackTrace();
        }

        remapperFunction = remapperFunction1;
    }

    public static File tryRemap(File file) {
        return remapperFunction.apply(file.toPath()).toFile();
    }
}
