dependencies {
    implementation("com.clickhouse:clickhouse-jdbc:0.7.2") {
        // ClickHouse JDBC pulls in org.lz4:lz4-pure-java which conflicts with the lz4 capability claimed by
        // at.yawk.lz4:lz4-java (transitively from kafka-clients). Excluding the older copy unblocks Gradle's
        // capability resolution without losing functionality — lz4 frame compression isn't on the hot path.
        exclude(group = "org.lz4", module = "lz4-pure-java")
    }
    implementation("com.github.spotbugs:spotbugs-annotations")
    implementation("com.zaxxer:HikariCP")
    implementation("io.micrometer:micrometer-core")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jdbc")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))
    implementation(project(":server:libs:config:app-config"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":server:libs:test:test-support"))
}
