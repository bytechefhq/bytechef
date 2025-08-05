dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation(project(":server:libs:test:test-int-support"))
}
