dependencies {
    implementation(project(":cli:cli-core"))
    implementation("org.springframework.shell:spring-shell-core:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation(project(":cli:cli-app"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
