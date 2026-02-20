dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-websocket")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
}
