dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework:spring-context")

    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))

    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-api"))
}
