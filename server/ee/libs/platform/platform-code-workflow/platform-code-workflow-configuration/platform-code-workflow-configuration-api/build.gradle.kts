dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-api"))
    api(project(":sdks:backend:java:workflow-api"))
    api(project(":server:libs:core:file-storage:file-storage-api"))

    implementation("org.springframework.data:spring-data-jdbc")
}
