dependencies {
    implementation("com.github.spotbugs:spotbugs-annotations")
    implementation("com.zaxxer:HikariCP")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.ai:spring-ai-autoconfigure-vector-store-pgvector")
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")

    testRuntimeOnly("org.postgresql:postgresql")
}
