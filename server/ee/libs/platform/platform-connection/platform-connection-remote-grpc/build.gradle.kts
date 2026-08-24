dependencies {
    api(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-grpc-proto"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation(libs.io.grpc.grpc.inprocess)
    testImplementation(libs.io.grpc.grpc.netty.shaded)
    testImplementation(libs.io.grpc.grpc.testing)
    testImplementation("org.springframework.boot:spring-boot-starter-grpc-server")
}
