version="1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-api"))
    implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-file-storage:platform-knowledge-base-file-storage-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
}
