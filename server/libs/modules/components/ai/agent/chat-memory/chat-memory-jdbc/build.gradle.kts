dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.zaxxer:HikariCP")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.ai:spring-ai-model-chat-memory-repository-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation(libs.org.springaicommunity.spring.ai.session.jdbc)
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation("com.h2database:h2")
}
