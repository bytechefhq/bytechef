version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(libs.org.springaicommunity.agents.agent.claude)
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation(project(":server:libs:core:commons:commons-util"))
}
