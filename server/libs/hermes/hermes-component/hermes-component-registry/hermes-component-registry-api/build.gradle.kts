dependencies {
    api(project(":server:libs:hermes:hermes-component:hermes-component-api"))
    api(project(":server:libs:hermes:hermes-connection:hermes-connection-api"))
    api(project(":server:libs:hermes:hermes-registry-api"))

    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
