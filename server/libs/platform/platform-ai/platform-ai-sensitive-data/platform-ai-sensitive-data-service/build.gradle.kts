dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations")

    implementation("org.jspecify:jspecify")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))

    api(project(":server:libs:platform:platform-ai:platform-ai-sensitive-data:platform-ai-sensitive-data-api"))
    api("org.springframework.ai:spring-ai-model")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
