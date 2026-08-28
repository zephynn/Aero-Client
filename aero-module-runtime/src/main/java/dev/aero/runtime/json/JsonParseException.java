package dev.aero.runtime.json;

/** Internal parse failure - callers should catch this and wrap it in a domain exception. */
public class JsonParseException extends RuntimeException {
    public JsonParseException(String message) {
        super(message);
    }
}
