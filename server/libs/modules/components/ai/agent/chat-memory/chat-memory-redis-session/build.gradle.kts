version = "1.0"

dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation("redis.clients:jedis")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-session-redis"))
}
