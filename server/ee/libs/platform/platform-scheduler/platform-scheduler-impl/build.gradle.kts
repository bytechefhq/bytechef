dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-quartz")
    implementation("org.quartz-scheduler:quartz")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.awaitility:awaitility")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    testRuntimeOnly("org.postgresql:postgresql")
}
