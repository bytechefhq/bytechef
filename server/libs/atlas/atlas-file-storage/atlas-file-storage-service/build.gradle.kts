dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-noop-service"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws"))
}
