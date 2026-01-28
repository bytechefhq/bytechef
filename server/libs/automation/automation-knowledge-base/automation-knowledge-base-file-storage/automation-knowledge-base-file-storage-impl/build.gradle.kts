dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-file-storage:automation-knowledge-base-file-storage-api"))
}
