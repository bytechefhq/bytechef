dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
