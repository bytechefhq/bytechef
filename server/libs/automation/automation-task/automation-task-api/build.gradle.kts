dependencies {
    api(project(":server:libs:core:commons:commons-util"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.data:spring-data-jdbc")

    compileOnly("com.github.spotbugs:spotbugs-annotations")
}
