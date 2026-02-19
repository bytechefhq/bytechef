dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-s3")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))

    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-api"))

    testImplementation(rootProject.libs.loki.logback.appender)
    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    testImplementation("io.awspring.cloud:spring-cloud-aws-dynamodb")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:config:jackson-config"))
}
