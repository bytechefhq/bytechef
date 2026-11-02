dependencies {
    implementation(libs.net.javacrumbs.shedlock.shedlock.provider.jdbc.template)
    implementation(libs.net.javacrumbs.shedlock.shedlock.spring)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))

    testImplementation("org.liquibase:liquibase-core")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))
    testImplementation(project(":server:libs:test:test-support"))

    testRuntimeOnly("org.postgresql:postgresql")
}
