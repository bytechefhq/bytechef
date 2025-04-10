dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-core:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.postgresql:postgresql:42.7.5")

    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))
}
