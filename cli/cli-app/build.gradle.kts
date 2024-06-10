plugins {
    id("com.bytechef.java-application-conventions")
}

application {
    mainClass.set("com.bytechef.cli.CliApplication")
}

tasks.compileJava {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

dependencies {
    implementation(project(":cli:commands:component"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.shell:spring-shell-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
