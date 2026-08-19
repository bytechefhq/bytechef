dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))

    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    // Test-only. The end-to-end gate test needs the agent action AND the gate element, which now live in separate
    // modules; it stays in this package because it exercises package-private members of the action. Production
    // classpaths are unaffected: components:ai:agent still has no production dependents.
    testImplementation(project(":server:libs:modules:components:ai:agent:utils"))
}

subprojects {
    dependencies {
        implementation(project(":server:libs:modules:components:ai:llm"))
    }
}
