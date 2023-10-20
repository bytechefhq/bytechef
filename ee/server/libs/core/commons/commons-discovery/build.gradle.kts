dependencies {
    api("org.springframework.cloud:spring-cloud-commons")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-core")
    implementation(project(":server:libs:core:commons:commons-util"))
}
