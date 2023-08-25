dependencies {
    api(project(":server:libs:hermes:hermes-file-storage:hermes-file-storage-api"))

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
