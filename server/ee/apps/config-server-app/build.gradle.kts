dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.cloud:spring-cloud-config-server")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:logback-config"))

    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
