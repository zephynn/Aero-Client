package dev.aero.core.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Only ever loaded if Mod Menu is installed and looks up an entrypoint named
 * "modmenu" (see {@code fabric.mod.json}) - Aero declares Mod Menu as a
 * "recommends", never a hard dependency, so this class is simply never
 * touched when Mod Menu is absent.
 */
public final class AeroModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AeroConfigScreen::new;
    }
}
