dependencies {
    api(project(":server:libs:platform:platform-plan:platform-plan-api"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${rootProject.libs.versions.spring.boot.get()}")

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
