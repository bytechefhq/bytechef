dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:core:error:error-api"))

    implementation("org.springframework.data:spring-data-jdbc")
}
