dependencies {
    api(project(":server:ee:libs:embedded:embedded-security:embedded-security-remote-grpc-proto"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":server:ee:libs:embedded:embedded-security:embedded-security-api"))
}
