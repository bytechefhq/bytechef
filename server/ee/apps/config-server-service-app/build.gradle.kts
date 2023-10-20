dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.cloud:spring-cloud-config-server")

    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
}
