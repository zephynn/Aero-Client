package dev.aero.runtime.pkg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scans a directory for development-format modules (plain {@code .jar}
 * files). A future {@code .aero} package would get its own discovery pass
 * feeding {@link dev.aero.api.ModulePackage} the same way.
 */
public final class ModuleDiscovery {

    private ModuleDiscovery() {
    }

    public static List<JarModulePackage> scan(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<JarModulePackage> found = new ArrayList<>();
        try (Stream<Path> entries = Files.list(directory)) {
            entries
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(p -> found.add(new JarModulePackage(p.toFile())));
        }
        return found;
    }

    public static JarModulePackage of(File jarFile) {
        return new JarModulePackage(jarFile);
    }
}
