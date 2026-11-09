dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-api"))
    implementation(project(":server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-service"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework.security:spring-security-config")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
