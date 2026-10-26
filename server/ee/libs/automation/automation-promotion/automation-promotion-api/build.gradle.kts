dependencies {
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.jspecify:jspecify")
    implementation(project(":server:libs:core:exception:exception-api"))
}
