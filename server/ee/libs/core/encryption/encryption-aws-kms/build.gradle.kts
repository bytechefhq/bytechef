dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("software.amazon.awssdk:kms")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))

    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:test:test-int-support"))
}
