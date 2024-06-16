dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
