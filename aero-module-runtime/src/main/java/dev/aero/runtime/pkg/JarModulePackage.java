package dev.aero.runtime.pkg;

import dev.aero.api.ModuleManifest;
import dev.aero.api.ModulePackage;
import dev.aero.api.exception.ManifestException;
import dev.aero.runtime.manifest.ManifestParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Phase 1's only {@link ModulePackage} implementation: a plain development
 * jar containing compiled module classes plus a {@code module.json} manifest
 * at its root. The eventual {@code .aero} format (a zip of
 * {@code module.jar + module.json + icon.png + signature}) becomes a second
 * implementation of {@link ModulePackage} that unpacks itself and delegates
 * to this class for the inner jar - the manager and classloading code above
 * this interface do not need to change.
 */
public final class JarModulePackage implements ModulePackage {

    private static final String MANIFEST_ENTRY = "module.json";

    private final File jarFile;

    public JarModulePackage(File jarFile) {
        this.jarFile = jarFile;
    }

    public File jarFile() {
        return jarFile;
    }

    @Override
    public ModuleManifest manifest() throws ManifestException {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(MANIFEST_ENTRY);
            if (entry == null) {
                throw new ManifestException("No " + MANIFEST_ENTRY + " found in " + jarFile.getName());
            }
            try (InputStream in = jar.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return ManifestParser.parse(json);
            }
        } catch (IOException e) {
            throw new ManifestException("Could not read " + jarFile.getName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public URL[] classpathUrls() throws IOException {
        try {
            return new URL[]{jarFile.toURI().toURL()};
        } catch (MalformedURLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        // Nothing held open between calls - manifest() and classpathUrls() each
        // open and close their own JarFile/URL handles.
    }

    @Override
    public String toString() {
        return "JarModulePackage[" + jarFile + "]";
    }
}
