dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
