import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.com.google.protobuf)
}

val protobufVersion = libs.versions.protobuf.get()

val grpcVersion: String = configurations
    .detachedConfiguration(
        dependencies.platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"),
        dependencies.create("io.grpc:grpc-stub"))
    .incoming
    .resolutionResult
    .allComponents
    .first { component -> component.moduleVersion?.name == "grpc-stub" }
    .moduleVersion!!
    .version

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
        }
    }
}

dependencies {
    api(libs.com.google.protobuf.protobuf.java)
    api(libs.com.google.protobuf.protobuf.java.util)
    api(libs.io.grpc.grpc.protobuf)
    api(libs.io.grpc.grpc.stub)

    // grpc-generated stubs reference javax.annotation.Generated
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

// This module carries ONLY generated protobuf/gRPC sources; the repo's static-analysis tools must
// not run against generated code.
tasks.matching {
    it.name.startsWith("checkstyle") || it.name.startsWith("pmd") || it.name.startsWith("spotbugs")
}.configureEach {
    enabled = false
}
