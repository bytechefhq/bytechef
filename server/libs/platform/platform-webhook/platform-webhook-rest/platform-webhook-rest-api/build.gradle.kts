dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    api(project(":server:libs:platform:platform-component:platform-component-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-instance-api"))
    api(project(":server:libs:platform:platform-file-storage:platform-file-storage-api"))
    api(project(":server:libs:platform:platform-webhook:platform-webhook-api"))

    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
}
