dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-notification:platform-notification-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation(project(":server:libs:test:test-support"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
