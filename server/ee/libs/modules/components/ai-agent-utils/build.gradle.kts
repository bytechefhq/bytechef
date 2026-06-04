version="1.0"

dependencies {
    implementation(rootProject.libs.org.springaicommunity.spring.ai.agent.utils)
    implementation("org.springframework:spring-context")
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:modules:components:ai:agent:utils"))
    implementation(project(":server:libs:modules:components:script"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
}
