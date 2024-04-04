dependencies {
    api(project(":server:libs:platform:platform-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
