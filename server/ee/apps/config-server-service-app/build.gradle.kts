dependencies {
    implementation("org.springframework.cloud:spring-cloud-config-server")

    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
}
