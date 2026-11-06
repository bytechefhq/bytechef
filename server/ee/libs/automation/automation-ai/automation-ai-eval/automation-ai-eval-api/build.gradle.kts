dependencies {
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
