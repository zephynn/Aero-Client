plugins {
    id("java-library")
}

group = rootProject.findProperty("maven_group") as String
version = rootProject.findProperty("aero_version") as String

java {
    withSourcesJar()
}
