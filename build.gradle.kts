plugins {
    alias(libs.plugins.com.github.ben.manes.versions)
    id("com.bytechef.java-common-conventions")
    id("jacoco-report-aggregation")
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
}

versionCatalogUpdate {
    keep {
        versions.addAll("org-springframework-cloud-dependencies")
    }
}

subprojects {
    apply(plugin = "com.bytechef.java-common-conventions")

    dependencies {
        implementation(rootProject.libs.com.github.spotbugs.spotbugs.annotations)
    }
}

reporting {
    reports {
        @Suppress("UnstableApiUsage")
        val jacocoRootReport by registering(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)

            dependencies {
                project.subprojects
                    .filter { it.plugins.findPlugin("jacoco") != null }
                    .forEach { jacocoAggregation(it) }
            }
        }

    }
}
