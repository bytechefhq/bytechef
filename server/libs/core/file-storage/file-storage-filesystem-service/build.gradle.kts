dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
