dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:message:message-event:message-event-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
