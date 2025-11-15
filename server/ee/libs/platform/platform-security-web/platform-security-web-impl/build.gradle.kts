dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
