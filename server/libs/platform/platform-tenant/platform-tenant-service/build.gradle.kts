dependencies {
    api(project(":server:libs:platform:platform-tenant:platform-tenant-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
