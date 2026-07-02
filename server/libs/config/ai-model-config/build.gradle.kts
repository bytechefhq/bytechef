dependencies {
    implementation("com.anthropic:anthropic-java-client-okhttp")
    implementation("com.openai:openai-java-client-okhttp")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-api"))
}
