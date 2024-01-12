dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":ee:server:libs:core:commons:commons-rest-client"))
}
