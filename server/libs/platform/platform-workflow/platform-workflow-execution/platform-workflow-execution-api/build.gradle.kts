plugins {
    `java-test-fixtures`
}

configurations.testFixturesImplementation {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    api("org.springframework.data:spring-data-commons")
    api("org.springframework.security:spring-security-core")
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    api(project(":server:libs:core:evaluator:evaluator-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))

    testImplementation("org.mockito:mockito-core")
    testImplementation(project(":server:libs:test:test-support"))
}
