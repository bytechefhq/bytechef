dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("tools.jackson.core:jackson-databind")
    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-data-redis")
    implementation("org.springframework.boot:spring-boot-web-server")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation("org.springframework.cloud:spring-cloud-config-client")
    implementation("org.springframework.data:spring-data-redis")
    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))
}
