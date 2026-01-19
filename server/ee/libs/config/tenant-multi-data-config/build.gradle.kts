dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("com.zaxxer:HikariCP")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jdbc")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")

    testRuntimeOnly("com.zaxxer:HikariCP")
    testRuntimeOnly("org.postgresql:postgresql")
}
