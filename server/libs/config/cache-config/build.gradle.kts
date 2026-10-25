dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.jspecify:jspecify")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-cache")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
