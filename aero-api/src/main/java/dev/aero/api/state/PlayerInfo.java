package dev.aero.api.state;

/** Read-only snapshot of the local player. {@code null} when not in a world. */
public record PlayerInfo(double x, double y, double z, float health, float maxHealth, String name) {
}
