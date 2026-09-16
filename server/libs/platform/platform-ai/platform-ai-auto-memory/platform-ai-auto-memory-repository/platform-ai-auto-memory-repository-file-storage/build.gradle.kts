dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-api"))

    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-test-support"))
    testImplementation(project(":server:libs:test:test-support"))

    testImplementation(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-api"))
    testImplementation(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-impl"))
}
