package dev.aero.api.state;

import java.util.List;

/** Read-only snapshot of the current world/dimension. {@code null} when not in a world. */
public record WorldInfo(String dimensionId, List<EntityInfo> nearbyEntities) {
}
