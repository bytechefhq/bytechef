dependencies {
    implementation("io.micrometer:micrometer-core")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    api(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
