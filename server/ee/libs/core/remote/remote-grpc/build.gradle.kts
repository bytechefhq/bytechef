plugins {
    `java-test-fixtures`
}

configurations.testFixturesImplementation {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework:spring-webmvc")
    compileOnly("org.springframework.graphql:spring-graphql")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-grpc-server")
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
    api("org.springframework.cloud:spring-cloud-commons")
    api("tools.jackson.core:jackson-databind")

    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api")) {
        isTransitive = false
    }

    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("io.grpc:grpc-services")
    testImplementation("org.springframework.graphql:spring-graphql")
    testImplementation(project(":server:libs:test:test-support"))
    testImplementation(
        testFixtures(project(":server:libs:platform:platform-security-web:platform-security-web-api")))
}
