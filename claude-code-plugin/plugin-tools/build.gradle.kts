plugins {
    id("com.bytechef.java-library-conventions")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
}

val generateMcpInstructions by tasks.registering(JavaExec::class) {
    group = "documentation"
    description = "Generates the management MCP server instruction fragments from the plugin skills."
    mainClass.set("com.bytechef.plugintools.PluginToolsMain")
    classpath = sourceSets.main.get().runtimeClasspath
    args(
        "mcp-instructions",
        "$rootDir/claude-code-plugin/bytechef-dev/skills",
        "$rootDir/server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md"
    )
}
