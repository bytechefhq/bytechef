dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-mail"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:ee:libs:platform:platform-user:platform-user-api"))
}
