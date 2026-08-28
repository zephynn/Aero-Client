plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
}

group = rootProject.findProperty("maven_group") as String
version = rootProject.findProperty("aero_version") as String

val minecraftVersion = rootProject.findProperty("minecraft_version") as String
val loaderVersion = rootProject.findProperty("loader_version") as String
val fabricApiVersion = rootProject.findProperty("fabric_api_version") as String
val modmenuVersion = rootProject.findProperty("modmenu_version") as String

repositories {
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("aero") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Fabric API supplies the three hooks aero-core bridges into the Aero
    // Module Runtime: client tick, HUD rendering, and keybinding.
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Mod Menu integration is optional at runtime (Aero works fine without
    // it installed - /aero list|enable|disable still work) - modCompileOnly
    // means it's on the compile classpath but never required or bundled.
    modCompileOnly("com.terraformersmc:modmenu:$modmenuVersion")

    // The pure-Java engine. Neither of these touches Minecraft, so they need
    // no remapping - their compiled classes are merged straight into
    // aero-core's own jar below instead of being published as separate mods.
    implementation(project(":aero-api"))
    implementation(project(":aero-module-runtime"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Aero is the *only* Fabric mod Fabric Loader ever loads - aero-api and
// aero-module-runtime are libraries embedded straight into aero.jar, not
// separate mods. This merges their compiled output in without pulling in a
// shading plugin, since (being pure Java, no third-party deps of their own)
// a plain class-copy is all that's needed.
tasks.named<Jar>("jar") {
    from(project(":aero-api").sourceSets.getByName("main").output)
    from(project(":aero-module-runtime").sourceSets.getByName("main").output)
}

tasks.processResources {
    val version = project.version
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}
