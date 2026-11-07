dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-stt:platform-ai-stt-api"))

    implementation(project(":server:libs:config:app-config"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
