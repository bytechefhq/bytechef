dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-api"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
