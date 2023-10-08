dependencies {
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    api("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
