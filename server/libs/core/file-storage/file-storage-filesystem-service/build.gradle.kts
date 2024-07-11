dependencies {
    api(project(":server:libs:core:file-storage:file-storage-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
