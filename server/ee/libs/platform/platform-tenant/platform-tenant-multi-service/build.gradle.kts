dependencies {
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
}
