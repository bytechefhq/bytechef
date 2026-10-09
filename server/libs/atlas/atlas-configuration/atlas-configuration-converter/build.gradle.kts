dependencies {
    api("tools.jackson.core:jackson-databind")
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))

    testImplementation(project(":server:libs:test:test-support"))
}
