dependencies {
    api(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-grpc-proto"))
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation(project(":server:libs:test:test-support"))
}
