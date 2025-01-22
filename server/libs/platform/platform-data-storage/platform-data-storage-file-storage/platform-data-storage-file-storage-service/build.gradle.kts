dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-file-storage:platform-data-storage-file-storage-api"))

    testImplementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
