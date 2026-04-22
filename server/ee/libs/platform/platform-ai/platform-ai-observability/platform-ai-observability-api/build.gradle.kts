dependencies {
    api("org.springframework.data:spring-data-commons")
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-llm-usage:platform-ai-llm-usage-api"))

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
