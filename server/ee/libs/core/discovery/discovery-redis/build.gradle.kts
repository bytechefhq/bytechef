dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.projectreactor:reactor-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.springframework.cloud.spring.cloud.commons)
    implementation("org.springframework.data:spring-data-redis")

    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))
}
