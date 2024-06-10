import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.org.sonarqube)
}

val sonarProperties = Properties()

sonarProperties.load(FileInputStream(File(projectDir.absolutePath + "/sonar-project.properties")))

sonarProperties.forEach { key, value ->
    sonar {
        properties {
            property(key.toString(), value.toString())
        }
    }
}

subprojects {
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.sonarqube")

    dependencies {
        implementation("org.springframework.shell:spring-shell-starter")
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.shell:spring-shell-dependencies:${rootProject.libs.versions.spring.shell.get()}")
        }
    }
}
