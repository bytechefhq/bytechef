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

val dataTableServiceProject = project(":server:libs:platform:platform-data-table:platform-data-table-service")

val syncLiquibaseCustomChanges by tasks.registering(Sync::class) {
    from(dataTableServiceProject.layout.projectDirectory.dir("src/main/java")) {
        include("com/bytechef/platform/data/table/configuration/migration/**")
    }

    into(layout.buildDirectory.dir("generated/sources/liquibaseCustomChanges"))
}

sourceSets {
    main {
        java.srcDir(syncLiquibaseCustomChanges)
    }
}

dependencies {
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    implementation(project(":server:ee:libs:core:tenant:tenant-multi-service"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-data-table:platform-data-table-api"))

    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":server:libs:test:test-int-support"))
}
