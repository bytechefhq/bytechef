dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:platform:platform-api"))

    api(project(":server:ee:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-api"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
