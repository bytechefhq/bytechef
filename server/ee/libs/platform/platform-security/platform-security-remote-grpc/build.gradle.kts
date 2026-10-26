dependencies {
    api(project(":server:ee:libs:platform:platform-security:platform-security-remote-grpc-proto"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
}
