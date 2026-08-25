dependencies {
    api(project(":server:libs:core:exception:exception-api"))

    api("com.github.spotbugs:spotbugs-annotations")
    api("tools.jackson.core:jackson-databind")

    implementation(project(":server:libs:platform:platform-api"))
}
