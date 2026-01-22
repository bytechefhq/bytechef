dependencies {
    implementation("de.siegmar:fastcsv:2.2.2")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-configuration:automation-data-table-configuration-api"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-execution:automation-data-table-execution-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation(project(":server:libs:test:test-int-support"))
}
