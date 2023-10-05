dependencies {
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    api("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
}
