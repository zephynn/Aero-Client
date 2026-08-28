package dev.aero.runtime.testkit;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Compiles a tiny {@link dev.aero.api.Module} implementation from a source
 * string and packages it (plus a {@code module.json}) into a real jar on
 * disk, so tests exercise the actual classloading/manifest-reading path
 * instead of mocking it. This is the only way to genuinely test "two
 * modules get isolated classloaders" and "a module's classloader becomes
 * unreachable after unload" without a second Gradle module full of fixture
 * jars checked into the repo.
 */
public final class TestModuleBuilder {

    private TestModuleBuilder() {
    }

    public static File compileModuleJar(Path workDir, String simpleClassName, String javaSource, String manifestJson)
            throws IOException {
        Path classesDir = Files.createDirectories(workDir.resolve(simpleClassName + "-classes"));
        Path sourceFile = workDir.resolve(simpleClassName + ".java");
        Files.writeString(sourceFile, javaSource, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler available (tests must run on a JDK, not a JRE)");
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));

            String classpath = System.getProperty("java.class.path");
            List<String> options = List.of("-classpath", classpath, "-d", classesDir.toString());

            JavaFileObject unit = new StringSource(simpleClassName, javaSource);
            boolean ok = compiler.getTask(null, fileManager, null, options, null, List.of(unit)).call();
            if (!ok) {
                throw new IllegalStateException("Failed to compile test fixture module " + simpleClassName);
            }
        }

        File jarFile = workDir.resolve(simpleClassName + ".jar").toFile();
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
            addTree(jarOut, classesDir, classesDir);
            jarOut.putNextEntry(new JarEntry("module.json"));
            jarOut.write(manifestJson.getBytes(StandardCharsets.UTF_8));
            jarOut.closeEntry();
        }
        return jarFile;
    }

    private static void addTree(JarOutputStream jarOut, Path root, Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                if (Files.isDirectory(path)) {
                    addTree(jarOut, root, path);
                } else {
                    String entryName = root.relativize(path).toString().replace(File.separatorChar, '/');
                    jarOut.putNextEntry(new JarEntry(entryName));
                    jarOut.write(Files.readAllBytes(path));
                    jarOut.closeEntry();
                }
            }
        }
    }

    private static final class StringSource extends SimpleJavaFileObject {
        private final String code;

        StringSource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                    JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
