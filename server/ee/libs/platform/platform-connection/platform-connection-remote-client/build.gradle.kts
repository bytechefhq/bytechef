dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
    implementation(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-grpc"))

    // grpc-stub/grpc-protobuf are API-only; a concrete channel transport is required at runtime for
    // ManagedChannelBuilder.forTarget(...). Runtime-only so it does not leak onto the compile classpath.
    runtimeOnly(libs.io.grpc.grpc.netty.shaded)
}
