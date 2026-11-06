dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-api"))
    implementation(project(":server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-service"))
}
