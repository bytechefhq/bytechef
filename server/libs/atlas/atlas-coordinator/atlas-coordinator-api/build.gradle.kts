dependencies {
    api(project(":server:libs:core:message:message-event:message-event-api"))
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
