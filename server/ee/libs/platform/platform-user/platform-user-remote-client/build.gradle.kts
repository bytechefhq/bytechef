dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
