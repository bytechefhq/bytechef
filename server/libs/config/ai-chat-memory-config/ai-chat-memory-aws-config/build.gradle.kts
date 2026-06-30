dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("software.amazon.awssdk:s3")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":spring-ai:spring-ai-model-chat-memory-repository-aws"))

    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
