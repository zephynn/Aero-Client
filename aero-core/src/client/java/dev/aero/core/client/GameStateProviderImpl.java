package dev.aero.core.client;

import dev.aero.api.state.EntityInfo;
import dev.aero.api.state.PlayerInfo;
import dev.aero.api.state.ScreenInfo;
import dev.aero.api.state.WorldInfo;
import dev.aero.runtime.GameStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The only class in Aero that hands Community Modules-facing game state,
 * translating real Minecraft objects into the Aero-controlled {@code
 * dev.aero.api.state} records. A module never receives {@code Minecraft},
 * {@code LocalPlayer}, {@code ClientLevel}, or {@code Entity} directly.
 */
public final class GameStateProviderImpl implements GameStateProvider {

    private static final int MAX_NEARBY_ENTITIES = 32;

    @Override
    public PlayerInfo player() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return null;
        }
        Vec3 pos = player.position();
        return new PlayerInfo(pos.x, pos.y, pos.z, player.getHealth(), player.getMaxHealth(), player.getName().getString());
    }

    @Override
    public WorldInfo world() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return null;
        }
        List<EntityInfo> nearby = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (nearby.size() >= MAX_NEARBY_ENTITIES) {
                break;
            }
            Vec3 pos = entity.position();
            nearby.add(new EntityInfo(entity.getId(), entity.getType().getDescription().getString(), pos.x, pos.y, pos.z));
        }
        return new WorldInfo(level.dimension().identifier().toString(), nearby);
    }

    @Override
    public ScreenInfo currentScreen() {
        return ScreenTracker.current();
    }
}
