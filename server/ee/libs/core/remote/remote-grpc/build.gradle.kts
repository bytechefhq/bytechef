dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-grpc-server")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))

    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("io.grpc:grpc-services")
}
