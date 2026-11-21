dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:libs:core:graphql:graphql-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
}
