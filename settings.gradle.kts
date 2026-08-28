pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "aero"

// Pure-Java engine modules: no Minecraft/Fabric dependency, buildable and
// unit-testable on their own. See aero-module-runtime/README.md.
include("aero-api")
include("aero-module-runtime")

// The actual Fabric mod. Aero is the *only* Fabric mod loaded by Fabric Loader;
// everything else in this repo is either a pure-Java library it embeds, or a
// Community Module that Aero loads dynamically at runtime (never bundled here).
include("aero-core")

// First-party Community Module used only to exercise the module runtime.
// Built as its own jar and dropped into the runtime "modules" directory -
// it is intentionally NOT a dependency of aero-core.
include("aero-official-modules:example-module")
