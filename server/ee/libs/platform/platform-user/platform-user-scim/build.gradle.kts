dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
