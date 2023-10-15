dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation("org.springframework.cloud:spring-cloud-config-client")
    implementation("org.springframework.data:spring-data-redis")
    implementation(project(":ee:server:libs:core:discovery:discovery-metadata-api"))
}
