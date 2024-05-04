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

    dependencies {
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}
