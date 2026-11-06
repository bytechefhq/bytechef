dependencies {
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-dataset:automation-ai-eval-dataset-api"))
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-dataset:automation-ai-eval-dataset-service"))
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-dataset:platform-ai-eval-dataset-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-dataset:platform-ai-eval-dataset-service"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
}
