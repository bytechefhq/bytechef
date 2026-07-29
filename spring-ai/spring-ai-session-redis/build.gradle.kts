plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

version = "1.0"

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))

    compileOnly("org.jspecify:jspecify")

    implementation(libs.findLibrary("org.springaicommunity.spring.ai.session.management").get())
    implementation("org.springframework.ai:spring-ai-model")
    implementation("redis.clients:jedis")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.testcontainers:junit-jupiter:${libs.findVersion("testcontainers").get()}")
}
