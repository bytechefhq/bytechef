dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))

    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(libs.org.wiremock.wiremock)
    testImplementation(project(":server:libs:test:test-support"))
}
