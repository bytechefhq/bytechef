dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-core")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:core:commons:commons-util"))
    testImplementation(project(":server:libs:core:evaluator:evaluator-impl"))
    testImplementation(project(":server:libs:test:test-support"))
}
