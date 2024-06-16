dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
