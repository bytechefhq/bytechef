dependencies {
    api("org.springframework.data:spring-data-commons")
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-experiment:platform-ai-eval-experiment-api"))

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
