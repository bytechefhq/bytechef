dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-api"))

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))
}
