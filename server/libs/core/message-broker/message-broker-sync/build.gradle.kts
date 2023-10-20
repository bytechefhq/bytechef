dependencies {
    api(project(":server:libs:core:message-broker:message-broker-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
