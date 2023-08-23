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
}
