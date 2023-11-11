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
        compileOnly(rootProject.libs.org.graalvm.polyglot.polyglot)
        compileOnly(rootProject.libs.org.graalvm.polyglot.java)
        compileOnly(rootProject.libs.org.graalvm.polyglot.js)
        compileOnly(rootProject.libs.org.graalvm.polyglot.python)
        compileOnly(rootProject.libs.org.graalvm.polyglot.ruby)

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
