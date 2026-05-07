dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("redis.clients:jedis")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
