package dev.aero.runtime.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * One classloader per module instance. The parent is Aero's own classloader
 * (which has the Aero API, and only the Aero API, visible on it), so:
 *
 * <ul>
 *   <li>{@code dev.aero.api.*} classes resolve to the single copy on Aero's
 *       classloader for every module (standard parent-first delegation) -
 *       they are never duplicated into a module's jar.</li>
 *   <li>A module's own classes are only visible through its own
 *       {@code ModuleClassLoader}, so two loaded modules cannot see or
 *       collide with each other's classes.</li>
 *   <li>Unloading a module is "drop every strong reference to its
 *       {@code ModuleClassLoader}" (module instance, listeners, tasks) and
 *       let the JVM garbage-collect it - see {@code ModuleManagerImpl}.</li>
 * </ul>
 */
public final class ModuleClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final String moduleId;

    public ModuleClassLoader(String moduleId, URL[] urls, ClassLoader aeroClassLoader) {
        super("aero-module[" + moduleId + "]", urls, aeroClassLoader);
        this.moduleId = moduleId;
    }

    public String moduleId() {
        return moduleId;
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            throw new UncheckedIOExceptionModuleClose(moduleId, e);
        }
    }

    private static final class UncheckedIOExceptionModuleClose extends RuntimeException {
        UncheckedIOExceptionModuleClose(String moduleId, IOException cause) {
            super("Failed to close classloader for module '" + moduleId + "'", cause);
        }
    }
}
