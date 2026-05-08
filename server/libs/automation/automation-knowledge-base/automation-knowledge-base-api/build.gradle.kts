dependencies {
    api("org.springframework.boot:spring-boot")
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-api"))

    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
}
