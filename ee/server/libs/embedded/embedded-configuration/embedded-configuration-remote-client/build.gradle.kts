dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))

    implementation(project(":ee:server:libs:core:commons:commons-rest-client"))
}
