version = "1.0"

dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-model-chat-memory-repository-aws"))
    implementation(project(":spring-ai:spring-ai-session-aws"))
}
