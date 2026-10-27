dependencies {
    api("org.springframework.cloud:spring-cloud-commons")

    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:test:test-support"))
}
