dependencies {
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
