dependencies {
    api(project(":server:libs:core:file-storage:file-storage-api"))

    implementation(project(":server:libs:core:commons:commons-data"))
    implementation("org.springframework.data:spring-data-relational")
}
