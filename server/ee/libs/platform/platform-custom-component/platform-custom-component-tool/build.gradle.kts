dependencies {
    implementation("org.springframework.ai:spring-ai-model")
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-api"))
    implementation(project(":server:libs:ai:ai-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(rootProject.libs.com.google.auto.service.auto.service.annotations)
    testImplementation("org.springframework:spring-webflux")
    annotationProcessor(rootProject.libs.com.google.auto.service.auto.service)
}
