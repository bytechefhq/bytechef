dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))

    implementation(project(":ee:server:libs:core:commons:commons-restclient"))
}
