dependencies {
    api(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation(project(":server:libs:test:test-support"))
}
