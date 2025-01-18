dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-web:platform-security-web-api"))

    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-api"))
}
