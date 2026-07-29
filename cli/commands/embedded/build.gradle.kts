dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations")

    implementation(project(":cli:cli-core"))
    implementation(project(":cli:clients:embedded-configuration"))
    implementation(project(":cli:clients:embedded-configuration-admin"))
    implementation(project(":cli:clients:embedded-execution"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.shell:spring-shell-core:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation(project(":cli:cli-app"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
