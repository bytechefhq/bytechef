dependencies {
    api(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(libs.commons.validator)
    implementation("org.eclipse.angus:angus-mail")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.thymeleaf:thymeleaf-spring6")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-mail")
    testImplementation(project(":server:libs:config:jdbc-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:config:messages-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
