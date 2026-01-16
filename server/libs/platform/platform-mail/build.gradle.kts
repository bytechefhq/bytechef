dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.eclipse.angus:angus-mail")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-mail")
    implementation("org.thymeleaf:thymeleaf-spring6")
    implementation(project(":server:libs:config:logback-config"))

    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    testImplementation(libs.loki.logback.appender)
    testImplementation(project(":server:libs:config:messages-config"))
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
}
