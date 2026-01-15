dependencies {
    api(project(":sdks:backend:java:component-api"))

    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.mockito:mockito-core:5.20.0")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
