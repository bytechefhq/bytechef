dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations")

    implementation(project(":cli:cli-core"))
    implementation(project(":cli:clients:platform-custom-component"))
    implementation(project(":cli:commands:component:init:openapi"))
    implementation("org.springframework.shell:spring-shell-core:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation(project(":cli:cli-app"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
