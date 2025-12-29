dependencies {
    implementation(project(":cli:commands:component:init:openapi"))
    implementation("org.springframework.shell:spring-shell-core:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation(project(":cli:cli-app"))
}
