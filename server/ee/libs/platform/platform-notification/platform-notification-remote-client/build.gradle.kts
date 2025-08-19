dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-notification:platform-notification-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
