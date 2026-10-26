group = "com.bytechef.migration"
description = ""

springBoot {
    mainClass.set("com.bytechef.migration.MigrationApplication")
}

val liquibaseConfigProject = project(":server:libs:config:liquibase-config")

val changelogResourceDirs = rootProject.subprojects
    .filter { it != project && it != liquibaseConfigProject }
    .map { it.layout.projectDirectory.dir("src/main/resources") }
    .filter { it.dir("config/liquibase").asFile.isDirectory }

tasks.named<ProcessResources>("processResources") {
    from(changelogResourceDirs) {
        include("config/liquibase/**")

        duplicatesStrategy = DuplicatesStrategy.FAIL
    }
}

dependencies {
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    implementation(project(":server:ee:libs:core:tenant:tenant-multi-service"))
    implementation(project(":server:libs:config:liquibase-config"))

    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":server:libs:test:test-int-support"))
}
