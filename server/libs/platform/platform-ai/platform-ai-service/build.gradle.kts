dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-core:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
}
