dependencies {
    api("tools.jackson.core:jackson-databind")
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.springframework:spring-test")
}
