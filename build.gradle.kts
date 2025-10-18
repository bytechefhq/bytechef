plugins {
    alias(libs.plugins.com.github.ben.manes.versions)
    id("com.bytechef.java-common-conventions")
    id("jacoco-report-aggregation")
    id("jvm-test-suite")
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    id("io.spring.dependency-management")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

versionCatalogUpdate {
    keep {
        versions.addAll("checkstyle", "findsecbugs", "gradle-git-properties", "jackson", "jacoco", "java", "jib-gradle-plugin", "pmd", "spotbugs", "spring-ai", "spring-boot", "spring-cloud-aws", "spring-cloud-dependencies", "spring-shell", "testcontainers")
    }
}


subprojects {
    apply(plugin = "com.bytechef.java-common-conventions")
    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        dependencies {
            dependency("com.github.spotbugs:spotbugs-annotations:[${rootProject.libs.versions.spotbugs.get()},)")
        }
    }

    dependencies {
        compileOnly(rootProject.libs.com.github.spotbugs.spotbugs.annotations)

        implementation("org.jspecify:jspecify")
        implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

        spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:${rootProject.libs.versions.findsecbugs.get()}")

        testCompileOnly(rootProject.libs.com.github.spotbugs.spotbugs.annotations)

        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}

reporting {
    reports {
        @Suppress("UnstableApiUsage")
        val jacocoRootReport by registering(JacocoCoverageReport::class) {
            testSuiteName = "test"

            dependencies {
                project.subprojects
                    .filter { it.plugins.findPlugin("jacoco") != null }
                    .forEach { jacocoAggregation(it) }
            }
        }
    }
}

tasks.matching { it.name.startsWith("spotless") }.configureEach {
    enabled = false
}
