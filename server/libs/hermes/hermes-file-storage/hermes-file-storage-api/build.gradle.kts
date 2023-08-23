dependencies {
    implementation("org.springframework:spring-core")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    testImplementation(project(":server:libs:hermes:hermes-file-storage:hermes-file-storage-base64-service"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:core:evaluator"))
}
