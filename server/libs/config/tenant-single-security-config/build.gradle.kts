dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation(libs.commons.validator)
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))

    testImplementation("org.apache.commons:commons-lang3")
    testImplementation("org.springframework:spring-context-support")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":server:libs:config:jdbc-config"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:config:security-config"))
    testImplementation(project(":server:libs:config:tenant-single-security-config"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
