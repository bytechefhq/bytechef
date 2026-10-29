dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:embedded:embedded-security:embedded-security-api"))
    implementation(project(":server:ee:libs:embedded:embedded-security:embedded-security-remote-grpc"))
}
