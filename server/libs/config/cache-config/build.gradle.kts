dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-redis")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
