dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-secrets-manager")
    implementation("io.awspring.cloud:spring-cloud-aws-autoconfigure")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:encryption:encryption-api"))

    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")
    testImplementation("io.awspring.cloud:spring-cloud-aws-dynamodb")
    testImplementation("io.awspring.cloud:spring-cloud-aws-s3")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
}
