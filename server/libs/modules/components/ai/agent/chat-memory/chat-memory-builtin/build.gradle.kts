dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session.management)
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("redis.clients:jedis")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-builtin-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
}
