version="1.0"

dependencies {
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.boot:spring-boot")
    api(project(":server:libs:platform:platform-ai:platform-ai-api"))

    implementation("org.springframework.ai:spring-ai-retry")
    implementation("org.springframework.boot:spring-boot-http-client")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation("org.mockito:mockito-core")
    testImplementation(project(":server:libs:test:test-support"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
