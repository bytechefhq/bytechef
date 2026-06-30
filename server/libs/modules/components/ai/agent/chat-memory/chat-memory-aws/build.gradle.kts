version = "1.0"

dependencies {
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-model-chat-memory-repository-aws"))
}
