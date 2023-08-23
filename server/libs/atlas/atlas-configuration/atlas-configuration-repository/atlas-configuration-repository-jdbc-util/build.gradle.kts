dependencies {
    api(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.data:spring-data-relational")
}
