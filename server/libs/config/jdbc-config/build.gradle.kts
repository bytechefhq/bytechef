dependencies {
    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.boot:spring-boot-jdbc")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
