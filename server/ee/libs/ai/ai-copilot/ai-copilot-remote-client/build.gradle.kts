dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
}
