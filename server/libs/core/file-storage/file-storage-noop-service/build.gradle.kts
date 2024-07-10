dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
}
