package dev.aero.api.exception;

/** Thrown by the module runtime for lifecycle failures (load/enable/disable/update). */
public class ModuleException extends Exception {
    public ModuleException(String message) {
        super(message);
    }

    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
