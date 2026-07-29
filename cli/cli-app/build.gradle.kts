plugins {
    id("com.bytechef.java-application-conventions")
    alias(libs.plugins.org.graalvm.buildtools.native)
}

graalvmNative {
    metadataRepository {
        enabled = true
    }
}

application {
    applicationName = "bytechef"
    mainClass.set("com.bytechef.cli.CliApplication")
}

tasks.compileJava {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

dependencies {
    implementation(project(":cli:cli-core"))
    implementation(project(":cli:commands:automation"))
    implementation(project(":cli:commands:component"))
    implementation(project(":cli:commands:config"))
    implementation(project(":cli:commands:embedded"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.shell:spring-shell-starter:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
