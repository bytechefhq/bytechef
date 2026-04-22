dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))

    // The eval-rule controller calls AiEvalExecutor + aiObservabilityTraceService for the historical-trace
    // re-run endpoint, so the graphql layer still depends on the gateway service module.
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-api"))
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
