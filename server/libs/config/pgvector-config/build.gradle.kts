dependencies {
    implementation("com.zaxxer:HikariCP")
    implementation("com.github.spotbugs:spotbugs-annotations")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jdbc")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.data:spring-data-relational")
    implementation(project(":server:libs:config:app-config"))
}
