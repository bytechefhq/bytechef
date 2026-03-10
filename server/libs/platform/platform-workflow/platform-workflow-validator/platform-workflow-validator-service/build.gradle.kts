dependencies {
    api(project(":server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-api"))
    api(project(":server:libs:platform:platform-api"))
    api("tools.jackson.core:jackson-databind")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-impl"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))
}
