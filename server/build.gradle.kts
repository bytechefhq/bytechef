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

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:" + rootProject.libs.versions.org.springframework.cloud.dependencies.get())
        }

        applyMavenExclusions(false)
    }

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("software.amazon.awssdk:bom:" + rootProject.libs.versions.awssdk.get())
        }

        applyMavenExclusions(false)
    }

    dependencies {
        implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${rootProject.libs.versions.spring.cloud.aws.get()}"))

        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}
