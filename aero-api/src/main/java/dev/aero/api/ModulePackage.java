package dev.aero.api;

import dev.aero.api.exception.ManifestException;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;

/**
 * Abstracts *where a module comes from*, so the runtime never hardcodes "a
 * module is a raw jar on disk". Phase 1 ships exactly one implementation
 * (a plain development jar); a future {@code .aero} package format - a zip
 * containing {@code module.jar}, {@code module.json}, an icon, a signature,
 * etc - is just another implementation of this interface, requiring no
 * change to {@link ModuleManager} or the classloading machinery.
 */
public interface ModulePackage extends Closeable {

    /** Parses and validates the manifest without loading any module code. */
    ModuleManifest manifest() throws ManifestException;

    /** URLs to hand to the module's classloader (never includes the Aero API itself). */
    URL[] classpathUrls() throws IOException;

    @Override
    void close();
}
