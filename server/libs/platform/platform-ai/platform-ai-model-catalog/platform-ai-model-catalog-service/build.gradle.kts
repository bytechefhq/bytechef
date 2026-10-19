import java.net.URI

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")

    api(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

spotless {
    json {
        target("src/**/*.json")
        targetExclude("src/main/resources/config/model-catalog/models-dev-api.json")
    }
}

tasks.register("refreshModelsDevSnapshot") {
    description = "Re-downloads the bundled models.dev api.json snapshot. Run manually, then commit the result."
    group = "build"
    notCompatibleWithConfigurationCache("Performs a network fetch on demand")

    doLast {
        val target = file("src/main/resources/config/model-catalog/models-dev-api.json")

        target.parentFile.mkdirs()

        URI("https://models.dev/api.json").toURL().openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }

        logger.lifecycle("Wrote ${target.length()} bytes to $target")
    }
}
