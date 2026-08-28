package dev.aero.core;

import net.fabricmc.api.ModInitializer;

/**
 * Aero is the *only* Fabric mod Fabric Loader ever loads. Everything else
 * (aero-api, aero-module-runtime, and every Community Module) lives inside
 * this one mod, either embedded in aero.jar or loaded dynamically at
 * runtime by the Aero Module Runtime - never as a second Fabric mod.
 */
public class AeroMod implements ModInitializer {

    @Override
    public void onInitialize() {
        Aero.info("Aero Phase 1 initializing");
    }
}
