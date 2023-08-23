import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.bytechef.java-application-conventions")
    id("com.bytechef.java-library-conventions")
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

    apply(from = rootDir.absolutePath + "/gradle/sonar.gradle")

    dependencies {
        annotationProcessor(rootProject.libs.info.picocli.picocli.codegen)

        implementation(rootProject.libs.ch.qos.logback.logback.core)
        implementation(rootProject.libs.ch.qos.logback.logback.classic)
        implementation(rootProject.libs.info.picocli)
        implementation(rootProject.libs.io.swagger.parser.v3.swagger.parser)

        constraints {
            implementation(rootProject.libs.ch.qos.logback.logback.core)
            implementation(rootProject.libs.ch.qos.logback.logback.classic)
        }
    }
}
