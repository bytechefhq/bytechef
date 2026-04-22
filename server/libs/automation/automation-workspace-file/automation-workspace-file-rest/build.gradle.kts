dependencies {
    api(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-api"))
    api(project(":server:libs:automation:automation-workspace-file:automation-workspace-file-service"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
