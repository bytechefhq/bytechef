dependencies {
    api("org.springframework.boot:spring-boot")
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:core:file-storage:file-storage-api"))
    api(project(":server:libs:core:message:message-api"))
    api(project(":server:libs:core:message:message-event:message-event-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    compileOnly("org.springframework.ai:spring-ai-vector-store")

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
}
