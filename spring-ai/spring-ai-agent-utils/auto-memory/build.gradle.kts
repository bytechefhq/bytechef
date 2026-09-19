plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

// Temporary, partial vendored fork of org.springaicommunity:spring-ai-agent-utils —
// only AutoMemoryTools + AutoMemoryToolsAdvisor, repackaged to com.bytechef.platform.ai.agent.memory
// and re-backed by a Spring Resource seam (DB storage) instead of java.nio.Files.
// See ../README.md for source provenance and the removal plan.
dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.ai:spring-ai-model")
    compileOnly("com.github.spotbugs:spotbugs-annotations")
    implementation("org.springframework:spring-core")
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
