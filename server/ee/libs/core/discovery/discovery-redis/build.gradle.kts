dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("tools.jackson.core:jackson-databind")
    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-data-redis")
    implementation("org.springframework.boot:spring-boot-web-server")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation("org.springframework.data:spring-data-redis")
    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))

    testImplementation("io.lettuce:lettuce-core")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
