dependencies {
    implementation("com.stripe:stripe-java:32.1.0")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-quartz")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.quartz-scheduler:quartz")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-billing:platform-billing-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:config:jackson-config"))
}
