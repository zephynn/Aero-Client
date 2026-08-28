package dev.aero.api.exception;

/** Thrown when a module manifest (module.json) is missing, malformed, or invalid. */
public class ManifestException extends ModuleException {
    public ManifestException(String message) {
        super(message);
    }

    public ManifestException(String message, Throwable cause) {
        super(message, cause);
    }
}
