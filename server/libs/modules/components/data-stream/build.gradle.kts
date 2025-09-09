dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.batch:spring-batch-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api"))

    testImplementation("org.springframework.batch:spring-batch-test")
}
