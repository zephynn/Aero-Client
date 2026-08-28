plugins {
    id("java")
}

group = "dev.aero.official"

dependencies {
    // compileOnly: aero-api is provided by Aero itself at runtime (loaded
    // from Aero's classloader) - it must never be bundled into a module jar.
    compileOnly(project(":aero-api"))

    // Test-only: drives the *real* v1Jar/v2Jar through the *real* runtime,
    // as an integration check that these three independently-built
    // artifacts (aero-api, aero-module-runtime, this module) actually work
    // together - not just the in-process fixtures aero-module-runtime's own
    // unit tests compile on the fly.
    testImplementation(project(":aero-module-runtime"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Two development-format module jars: v1 and v2 of the same module id
// ("example-module"), used to demonstrate install/enable/disable/update/
// uninstall against the real Aero Module Runtime. Each jar contains exactly
// one compiled entrypoint class plus its own module.json - never both.
tasks.register<Jar>("v1Jar") {
    group = "aero"
    description = "Builds example-module-1.0.0.jar (development module format)."
    archiveBaseName.set("example-module")
    archiveVersion.set("1.0.0")
    from(sourceSets.main.get().output) {
        include("dev/aero/official/example/ExampleModule.class")
    }
    from("src/main/manifests/module-v1.json") {
        rename { "module.json" }
    }
}

tasks.register<Jar>("v2Jar") {
    group = "aero"
    description = "Builds example-module-2.0.0.jar (development module format) - used to demonstrate a hot update."
    archiveBaseName.set("example-module")
    archiveVersion.set("2.0.0")
    from(sourceSets.main.get().output) {
        include("dev/aero/official/example/ExampleModuleV2.class")
    }
    from("src/main/manifests/module-v2.json") {
        rename { "module.json" }
    }
}

tasks.named("assemble") {
    dependsOn("v1Jar", "v2Jar")
}

tasks.test {
    dependsOn("v1Jar", "v2Jar")
    systemProperty("aero.test.v1Jar", tasks.named<Jar>("v1Jar").get().archiveFile.get().asFile.absolutePath)
    systemProperty("aero.test.v2Jar", tasks.named<Jar>("v2Jar").get().archiveFile.get().asFile.absolutePath)
}

// The plugin's default `jar` task would bundle both entrypoint classes into
// one (manifest-less, useless) jar; disable it in favor of v1Jar/v2Jar above.
tasks.named<Jar>("jar") {
    enabled = false
}
