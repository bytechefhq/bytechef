dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.springframework:spring-jdbc")
    implementation("software.amazon.awssdk:s3")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-jdbc-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-session-aws"))
}
