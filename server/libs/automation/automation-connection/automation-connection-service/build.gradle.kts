dependencies {
    implementation("com.github.spotbugs:spotbugs-annotations")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
}