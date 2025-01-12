dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-instance-api"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-api"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-api"))
}
