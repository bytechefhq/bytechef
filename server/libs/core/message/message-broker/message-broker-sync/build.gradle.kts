dependencies {
    api(project(":server:libs:core:message:message-broker:message-broker-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
}
