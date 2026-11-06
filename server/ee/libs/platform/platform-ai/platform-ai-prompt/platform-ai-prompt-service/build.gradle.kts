dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-prompt:platform-ai-prompt-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
