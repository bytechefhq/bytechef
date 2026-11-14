dependencies {
    api("org.springframework:spring-core")
    api("tools.jackson.core:jackson-databind")
    api(project(":server:libs:core:encryption:encryption-api"))
    api(project(":server:libs:core:error:error-api"))
    api(project(":server:libs:core:file-storage:file-storage-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-jdbc")
}
