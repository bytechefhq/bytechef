dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api(project(":server:libs:core:message-broker:message-broker-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
}
