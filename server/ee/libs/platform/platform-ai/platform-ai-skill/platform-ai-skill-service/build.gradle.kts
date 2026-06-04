dependencies {
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-api"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
}
