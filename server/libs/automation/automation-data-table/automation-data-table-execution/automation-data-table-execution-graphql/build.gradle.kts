dependencies {
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-configuration:automation-data-table-configuration-api"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-execution:automation-data-table-execution-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework:spring-web")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation(libs.org.wiremock.wiremock)
    testImplementation(project(":server:libs:test:test-int-support"))
}
