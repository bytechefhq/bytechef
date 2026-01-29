dependencies {
    implementation("org.springframework.ai:spring-ai-autoconfigure-vector-store-pgvector")
    implementation("org.springframework.ai:spring-ai-commons")
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    implementation(project(":server:ee:libs:config:tenant-multi-pgvector-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
}
