dependencies {
    api(project(":server:libs:platform:platform-user:platform-user-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(libs.commons.validator)
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-crypto")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation(project(":server:libs:config:jdbc-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
