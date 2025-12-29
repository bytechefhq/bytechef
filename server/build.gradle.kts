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
    apply(plugin = "org.sonarqube")

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:" + rootProject.libs.versions.spring.cloud.dependencies.get())
        }

        applyMavenExclusions(false)
    }

    dependencies {
        implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${rootProject.libs.versions.spring.cloud.aws.get()}"))
        implementation(platform("org.springframework.ai:spring-ai-bom:${rootProject.libs.versions.spring.ai.get()}"))
    }

    dependencyManagement {
        dependencies {
            dependency("org.testcontainers:junit-jupiter:${rootProject.libs.versions.testcontainers.get()}")
            dependency("org.testcontainers:localstack:${rootProject.libs.versions.testcontainers.get()}")
            dependency("org.testcontainers:postgresql:${rootProject.libs.versions.testcontainers.get()}")
        }
    }
}
