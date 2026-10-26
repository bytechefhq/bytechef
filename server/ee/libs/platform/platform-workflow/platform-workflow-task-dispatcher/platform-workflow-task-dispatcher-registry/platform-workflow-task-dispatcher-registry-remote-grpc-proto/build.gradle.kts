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
    api(project(":server:ee:libs:platform:platform-component:platform-component-remote-grpc-proto"))
    api("com.google.protobuf:protobuf-java")
    api("io.grpc:grpc-protobuf")
    api("io.grpc:grpc-stub")

    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

tasks.matching {
    it.name.startsWith("checkstyle") || it.name.startsWith("pmd") || it.name.startsWith("spotbugs")
}.configureEach {
    enabled = false
}
