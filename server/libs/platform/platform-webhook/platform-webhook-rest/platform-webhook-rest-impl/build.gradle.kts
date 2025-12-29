dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(project(":server:libs:test:test-int-support"))
}
