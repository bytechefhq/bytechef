dependencies {
    annotationProcessor(rootProject.libs.com.google.auto.service.auto.service)

    implementation("org.slf4j:slf4j-api")
    implementation(rootProject.libs.com.google.auto.service.auto.service.annotations)
    implementation(libs.org.springaicommunity.spring.ai.agent.utils)
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation(project(":server:libs:ai:ai-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework:spring-webflux")
    testImplementation(project(":server:libs:test:test-support"))
}
