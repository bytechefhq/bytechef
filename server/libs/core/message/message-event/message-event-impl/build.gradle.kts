dependencies {
    api(project(":server:libs:core:message:message-event:message-event-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
