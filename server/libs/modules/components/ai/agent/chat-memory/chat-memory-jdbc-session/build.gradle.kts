dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation(libs.org.springaicommunity.spring.ai.session.jdbc)
    implementation("org.springframework:spring-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
}
