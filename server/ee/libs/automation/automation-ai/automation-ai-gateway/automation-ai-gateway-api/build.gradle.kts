dependencies {
    api("org.springframework.data:spring-data-commons")

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
}
