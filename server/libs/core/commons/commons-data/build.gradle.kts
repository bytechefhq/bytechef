dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api(project(":server:libs:core:encryption:encryption-api"))

    implementation("org.springframework:spring-core")
}
