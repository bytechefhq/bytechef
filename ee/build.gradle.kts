
subprojects {
    apply(plugin = "io.spring.dependency-management")

    ext["commons-lang3.version"] = "3.13.0"

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:" + rootProject.libs.versions.org.springframework.cloud.dependencies.get())
        }

        applyMavenExclusions(false)
    }
}
