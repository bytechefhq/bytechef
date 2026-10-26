dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-llm-usage:platform-ai-llm-usage-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
}
