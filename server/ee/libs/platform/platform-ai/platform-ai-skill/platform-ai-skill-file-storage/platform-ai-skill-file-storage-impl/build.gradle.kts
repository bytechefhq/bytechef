dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-api"))
}
