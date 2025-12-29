dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(files("../libs/ag-ui/core-0.0.1.jar"))
    implementation(files("../libs/ag-ui/server-0.0.1.jar"))
    implementation(files("../libs/ag-ui/spring-0.0.1.jar"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
