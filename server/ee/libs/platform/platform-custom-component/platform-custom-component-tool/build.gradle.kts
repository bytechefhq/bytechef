dependencies {
    implementation("org.springframework.ai:spring-ai-model")
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    testImplementation("org.springframework:spring-webflux")
}
