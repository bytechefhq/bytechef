dependencies {
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-api"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")

    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-impl"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(testFixtures(project(":server:libs:platform:platform-security-web:platform-security-web-api")))
}
