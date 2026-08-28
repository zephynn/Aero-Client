package dev.aero.api.state;

/** Read-only snapshot of one nearby entity. */
public record EntityInfo(int id, String typeName, double x, double y, double z) {
}
