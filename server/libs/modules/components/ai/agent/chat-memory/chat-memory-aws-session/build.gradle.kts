version = "1.0"

dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-session-aws"))
}
