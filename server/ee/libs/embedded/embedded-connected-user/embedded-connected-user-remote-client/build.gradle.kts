dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:embedded:embedded-connected-user:embedded-connected-user-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
