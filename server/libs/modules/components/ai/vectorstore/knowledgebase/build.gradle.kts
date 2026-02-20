version="1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
}
