dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.modelcontextprotocol.sdk:mcp:${libs.versions.io.modelcontextprotocol.sdk.get()}")
    implementation("org.springframework.ai:mcp-spring-webmvc")
    implementation("org.springframework:spring-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")
}
