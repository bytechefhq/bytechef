dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:annotation-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
