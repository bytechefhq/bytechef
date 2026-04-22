dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.ai:spring-ai-model")
    implementation(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-api"))
    implementation(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-service"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
