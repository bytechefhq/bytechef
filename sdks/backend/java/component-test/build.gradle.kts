dependencies {
    api(project(":sdks:backend:java:component-api"))

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
