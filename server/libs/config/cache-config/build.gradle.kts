dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-cache")
    implementation("org.springframework.data:spring-data-redis")
    implementation("org.springframework:spring-tx")
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:tenant:tenant-api"))
}
