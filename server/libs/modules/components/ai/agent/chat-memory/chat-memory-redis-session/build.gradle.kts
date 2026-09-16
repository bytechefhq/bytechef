version = "1.0"

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation("redis.clients:jedis")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":spring-ai:spring-ai-session-redis"))
}
