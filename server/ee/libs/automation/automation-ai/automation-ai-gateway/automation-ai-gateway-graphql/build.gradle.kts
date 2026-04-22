dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
}
