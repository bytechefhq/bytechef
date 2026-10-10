dependencies {
    api(project(":server:libs:core:exception:exception-api"))
    api(project(":server:libs:core:file-storage:file-storage-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.data:spring-data-relational")
}
