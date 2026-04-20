dependencies {
    implementation(libs.com.github.mizosoft.methanol)
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation(project(":server:libs:config:app-config"))
}
