dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
