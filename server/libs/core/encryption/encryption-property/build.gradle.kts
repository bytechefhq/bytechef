dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:encryption:encryption-api"))

    testImplementation(project(":server:libs:test:test-int-support"))
}
