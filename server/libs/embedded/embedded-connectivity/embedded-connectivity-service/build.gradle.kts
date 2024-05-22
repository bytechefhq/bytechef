dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:embedded:embedded-connectivity:embedded-connectivity-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-registry:platform-component-registry-api"))
}
