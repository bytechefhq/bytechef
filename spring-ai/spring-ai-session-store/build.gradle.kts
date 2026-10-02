plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

version = "1.0"

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))

    compileOnly("org.jspecify:jspecify")

    api(libs.findLibrary("org.springaicommunity.spring.ai.session").get())
    api("org.springframework.ai:spring-ai-model")
    api("tools.jackson.core:jackson-databind")

    implementation("org.slf4j:slf4j-api")

    testImplementation("ch.qos.logback:logback-classic")
}
