plugins {
    id("java-library")
}

group = rootProject.findProperty("maven_group") as String
version = rootProject.findProperty("aero_version") as String

dependencies {
    api(project(":aero-api"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
