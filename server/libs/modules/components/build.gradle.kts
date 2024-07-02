
plugins {
    kotlin("jvm") version "1.9.20"
}

subprojects {
    tasks.jar {
        archiveBaseName.set("component-" + project.name)
    }

    dependencies {
        annotationProcessor(rootProject.libs.com.google.auto.service.auto.service)

        implementation("org.apache.commons:commons-lang3")
        implementation(rootProject.libs.com.google.auto.service.auto.service.annotations)
        implementation(project(":sdks:backend:java:component-api"))

        testImplementation(rootProject.libs.org.json)
        testImplementation(project(":server:libs:test:test-support"))
    }

    apply(plugin = "com.bytechef.documentation-generator")
}
