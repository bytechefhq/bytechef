dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("com.zaxxer:HikariCP")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
}
