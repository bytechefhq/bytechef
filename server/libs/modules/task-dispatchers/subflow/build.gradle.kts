version="1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation(project(":server:libs:test:test-support"))
}
