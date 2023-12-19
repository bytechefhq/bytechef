dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:hermes:hermes-configuration:hermes-configuration-api"))

    implementation(project(":ee:server:libs:core:commons:commons-rest-client"))
}
