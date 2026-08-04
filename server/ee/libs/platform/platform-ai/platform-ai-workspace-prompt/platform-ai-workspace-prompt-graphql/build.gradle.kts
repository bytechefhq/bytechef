dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:graphql:graphql-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
