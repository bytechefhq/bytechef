dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.slf4j:slf4j-api")
    implementation(libs.commons.validator)
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:annotation-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
