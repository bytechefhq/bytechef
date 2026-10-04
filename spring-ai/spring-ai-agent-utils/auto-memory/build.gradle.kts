plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))
    api("org.springframework.ai:spring-ai-model")
    compileOnly("com.github.spotbugs:spotbugs-annotations")
    implementation("org.springframework:spring-core")
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
