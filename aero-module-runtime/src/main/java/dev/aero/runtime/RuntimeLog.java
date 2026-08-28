package dev.aero.runtime;

/**
 * Sink the runtime logs through. {@code aero-core} supplies an implementation
 * backed by its SLF4J logger; a default no-dependency implementation
 * ({@link #systemOut()}) is used by tests and anywhere else that hasn't
 * wired one up.
 */
public interface RuntimeLog {
    void info(String message);

    void warn(String message);

    void error(String message, Throwable cause);

    static RuntimeLog systemOut() {
        return new RuntimeLog() {
            @Override
            public void info(String message) {
                System.out.println("[Aero] " + message);
            }

            @Override
            public void warn(String message) {
                System.out.println("[Aero] [WARN] " + message);
            }

            @Override
            public void error(String message, Throwable cause) {
                System.out.println("[Aero] [ERROR] " + message);
                if (cause != null) {
                    cause.printStackTrace();
                }
            }
        };
    }
}
