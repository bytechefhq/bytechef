dependencies {
    api(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-dynamodb")
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:config:app-config"))

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
}
