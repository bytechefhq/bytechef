dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    api(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-service"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
