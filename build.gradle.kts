plugins {
    alias(libs.plugins.com.github.ben.manes.versions)
    id("com.bytechef.java-common-conventions")
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
}

versionCatalogUpdate {
    keep {
        versions.addAll("org-springframework-cloud-dependencies")
    }
}

subprojects {
    apply(plugin = "com.bytechef.java-common-conventions")

    configurations {
        all {
            // https://github.com/testcontainers/testcontainers-java/issues/970
            /* exclude junit 4 dependencies */
            // exclude group: 'junit', module: 'junit'
            // exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
        }
    }

    dependencies {
        compileOnly(rootProject.libs.org.graalvm.sdk.graal.sdk)

        implementation(rootProject.libs.com.github.spotbugs.spotbugs.annotations)
    }
}

//val publishedProjects = subprojects.findAll()
//
//val jacocoRootReport by tasks.registering(JacocoReport::class) {
//    description = "Generates an aggregate report from all subprojects"
//    group = "Coverage reports"
//
//    dependsOn(publishedProjects.test)
//
//    additionalSourceDirs.setFrom(files(publishedProjects.sourceSets.main.allSource.srcDirs))
//    sourceDirectories.setFrom(files(publishedProjects.sourceSets.main.allSource.srcDirs))
//    classDirectories.setFrom(files(publishedProjects.sourceSets.main.output))
//    executionData.setFrom(files(publishedProjects.jacocoTestReport.executionData))
//
//    reports {
//        html.required.set(true) // human readable
//        xml.required.set(true) // required by codecov
//    }
//}
//
//wrapper {
//    gradleVersion = "8.3"
//}
