import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.com.google.protobuf)
}

val protobufVersion = libs.versions.protobuf.get()
val grpcVersion = libs.versions.grpc.get()

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
    api("com.google.protobuf:protobuf-java")
    api("io.grpc:grpc-protobuf")
    api("io.grpc:grpc-stub")
    api(project(":server:ee:libs:automation:automation-configuration:automation-configuration-remote-grpc-proto"))

    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

tasks.matching {
    it.name.startsWith("checkstyle") || it.name.startsWith("pmd") || it.name.startsWith("spotbugs")
}.configureEach {
    enabled = false
}
