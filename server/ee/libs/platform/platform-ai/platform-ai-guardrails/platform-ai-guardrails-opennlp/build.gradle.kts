dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.apache.opennlp.opennlp.tools)
    implementation(project(":server:libs:platform:platform-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation(project(":server:libs:test:test-support"))
}
