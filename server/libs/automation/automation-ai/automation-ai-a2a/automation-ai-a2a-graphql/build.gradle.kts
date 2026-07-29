dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-a2a:automation-ai-a2a-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
}
