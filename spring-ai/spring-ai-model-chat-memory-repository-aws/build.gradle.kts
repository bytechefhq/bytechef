plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

version = "1.0"

dependencies {
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${libs.findVersion("spring-cloud-aws").get()}"))
    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))

    compileOnly("org.jspecify:jspecify")

    implementation("org.springframework.ai:spring-ai-model")
    implementation("software.amazon.awssdk:s3")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.testcontainers:localstack:${libs.findVersion("testcontainers").get()}")
    testImplementation("org.testcontainers:junit-jupiter:${libs.findVersion("testcontainers").get()}")
    testImplementation("software.amazon.awssdk:s3")
}
