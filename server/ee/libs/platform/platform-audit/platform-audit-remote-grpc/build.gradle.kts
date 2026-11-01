dependencies {
    api(project(":server:ee:libs:platform:platform-audit:platform-audit-remote-grpc-proto"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:platform:platform-audit:platform-audit-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
