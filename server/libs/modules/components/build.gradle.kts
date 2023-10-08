subprojects {
    tasks.jar {
        archiveBaseName.set("component-" + project.name)
    }

    dependencies {
        annotationProcessor(rootProject.libs.com.google.auto.service.auto.service)

        implementation("org.apache.commons:commons-lang3")
        implementation("org.slf4j:slf4j-api")
        implementation(rootProject.libs.com.google.auto.service.auto.service.annotations)
        implementation(project(":server:libs:hermes:hermes-component:hermes-component-api"))

        testImplementation(rootProject.libs.org.json)
        testImplementation(project(":server:libs:test:test-support"))
    }
}
