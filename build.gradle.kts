// Root build script. Only aero-core (the Fabric mod) applies the Loom plugin;
// the pure-Java modules (aero-api, aero-module-runtime, example-module) use
// the plain `java` plugin so they build and test without touching Minecraft.

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    repositories {
        mavenCentral()
    }
}
