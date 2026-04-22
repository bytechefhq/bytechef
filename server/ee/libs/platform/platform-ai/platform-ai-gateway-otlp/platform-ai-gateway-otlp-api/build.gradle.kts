dependencies {
    api("io.opentelemetry.proto:opentelemetry-proto:1.3.2-alpha")
    api("com.google.protobuf:protobuf-java:3.25.5")

    implementation("org.apache.commons:commons-lang3")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
