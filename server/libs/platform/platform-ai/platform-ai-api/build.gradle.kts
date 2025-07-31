dependencies {
    api("io.projectreactor:reactor-core")

    implementation("org.springframework:spring-core")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:platform:platform-api"))
}
