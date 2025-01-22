dependencies {
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
