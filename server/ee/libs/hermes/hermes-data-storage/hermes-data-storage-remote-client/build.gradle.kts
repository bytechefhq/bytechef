dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:hermes:hermes-data-storage:hermes-data-storage-api"))

    implementation(project(":server:ee:libs:core:commons:commons-webclient"))
}
