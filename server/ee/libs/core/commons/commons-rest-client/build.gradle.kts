dependencies {
    api("org.springframework:spring-web")

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
