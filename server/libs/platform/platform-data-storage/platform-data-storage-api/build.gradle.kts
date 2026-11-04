dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:libs:platform:platform-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-relational")
    implementation("org.jspecify:jspecify")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-tx")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:test:test-support"))
}
