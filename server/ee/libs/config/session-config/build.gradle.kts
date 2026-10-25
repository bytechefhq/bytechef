dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-session-data-redis")
    implementation("org.springframework.boot:spring-boot-session-jdbc")
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis-control"))

    testImplementation("com.zaxxer:HikariCP")
    testImplementation("io.lettuce:lettuce-core")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.liquibase:liquibase-core")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework.boot:spring-boot-data-redis")
    testImplementation("org.springframework.boot:spring-boot-jdbc")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))

    testRuntimeOnly("org.postgresql:postgresql")
}
