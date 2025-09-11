dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api("org.springframework:spring-core")
    api(project(":server:libs:core:encryption:encryption-api"))
    api(project(":server:libs:core:error:error-api"))
    api(project(":server:libs:core:file-storage:file-storage-api"))
}
