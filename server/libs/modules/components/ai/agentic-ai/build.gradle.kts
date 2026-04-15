plugins {
    kotlin("jvm") version "2.3.0"
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    dependsOn(tasks.named("compileJava"))

    classes = fileTree(layout.buildDirectory.dir("classes/java/main"))
}

dependencies {
    implementation("com.embabel.agent:embabel-agent-api:0.3.5")
    implementation("com.embabel.agent:embabel-agent-starter-platform:0.3.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:ai:ai-tool-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))

    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
}
