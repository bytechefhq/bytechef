dependencies {
    implementation("org.springframework:spring-context")

    implementation(project(":server:ee:libs:platform:platform-external-invocation-log:platform-external-invocation-log-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
