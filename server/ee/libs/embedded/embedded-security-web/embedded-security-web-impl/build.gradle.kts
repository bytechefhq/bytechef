dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.apache.commons:commons-lang3")
    implementation(libs.jjwt.api)
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:ee:libs:embedded:embedded-security:embedded-security-api"))

    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))

    testRuntimeOnly(libs.jjwt.impl)
    testRuntimeOnly(libs.jjwt.jackson)
}
