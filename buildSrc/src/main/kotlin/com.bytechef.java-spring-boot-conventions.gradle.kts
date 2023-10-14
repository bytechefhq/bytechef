/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("com.bytechef.java-application-conventions")

    id("com.google.cloud.tools.jib")
    id("com.gorylenko.gradle-git-properties")
    id("org.springframework.boot")
}

//https://melix.github.io/blog/2021/03/version-catalogs-faq.html#_can_i_use_the_version_catalog_in_buildsrc
val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
var profiles = ""

if (project.hasProperty("prod")) {
    profiles = "prod"

    if (project.hasProperty("api-docs")) {
        profiles += ",api-docs"
    }

    configure<org.springframework.boot.gradle.dsl.SpringBootExtension> {
        buildInfo()
    }
} else {
    dependencies {
        "developmentOnly"("org.springframework.boot:spring-boot-devtools:${libs.findVersion("spring-boot").get()}")
    }

    profiles = "dev"

    configure<org.springframework.boot.gradle.dsl.SpringBootExtension> {
        buildInfo {
            properties {
                time = null
            }
        }
    }
}

tasks.withType(org.springframework.boot.gradle.tasks.run.BootRun::class) {
    args = listOf("--spring.profiles.active=$profiles")
}

val processResources by tasks.existing(ProcessResources::class) {
    inputs.property("version", version)
    inputs.property("springProfiles", profiles)

    filesMatching("**/application.yml") {
        filter {
            it.replace("#project.version#", version.toString())
        }
        filter {
            it.replace("#spring.profiles.active#", profiles)
        }
    }
}

val bootJar by tasks.existing {
    dependsOn(processResources)
}

val compileJava by tasks.existing {
    dependsOn(processResources)
}

val bootBuildInfo by tasks.existing

tasks.processResources {
    dependsOn(bootBuildInfo)
}

if (project.hasProperty("native")) {
    configure<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
        builder.set("paketobuildpacks/builder:tiny")
        environment.set(
            mapOf( "BP_NATIVE_IMAGE" to "true")
        )
    }

//        configure<org.graalvm.buildtools.gradle.dsl.NativeImageOptions> {
//            metadataRepository {
//                enabled.set(true)
//            }
//        }
}

defaultTasks("bootRun")

configure<com.gorylenko.GitPropertiesPluginExtension> {
    failOnNoGitDirectory = false
    setKeys(listOf("git.branch", "git.build.version", "git.commit.id", "git.commit.id.abbrev", "git.commit.id.describe"))
}

configure<com.google.cloud.tools.jib.gradle.JibExtension> {
    from {
        image = "ghcr.io/graalvm/graalvm-community:20.0.2-ol9"
    }
    to {
        image = "bytechef/bytechef-" + project.name + ":latest"
    }
}
