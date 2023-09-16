/*
 * This file was generated by the Gradle "init" task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.1/userguide/multi_project_builds.html
 */

pluginManagement {
    repositories {
        gradlePluginPortal()

        mavenCentral()

        maven {
            url = uri("https://repo.spring.io/release")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "bytechef"

include("cli:cli-app")
include("cli:commands:component")

include("server:apps:server-app")

include("server:libs:atlas:atlas-configuration:atlas-configuration-api")
include("server:libs:atlas:atlas-configuration:atlas-configuration-converter")
include("server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-api")
include("server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-git")
include("server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc")
include("server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-resource")
include("server:libs:atlas:atlas-configuration:atlas-configuration-service")
include("server:libs:atlas:atlas-coordinator:atlas-coordinator-api")
include("server:libs:atlas:atlas-coordinator:atlas-coordinator-config")
include("server:libs:atlas:atlas-coordinator:atlas-coordinator-impl")
include("server:libs:atlas:atlas-execution:atlas-execution-api")
include("server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api")
include("server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc")
include("server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-memory")
include("server:libs:atlas:atlas-execution:atlas-execution-service")
include("server:libs:atlas:atlas-file-storage:atlas-file-storage-api")
include("server:libs:atlas:atlas-file-storage:atlas-file-storage-service")
include("server:libs:atlas:atlas-sync-executor")
include("server:libs:atlas:atlas-worker:atlas-worker-api")
include("server:libs:atlas:atlas-worker:atlas-worker-config")
include("server:libs:atlas:atlas-worker:atlas-worker-impl")

include("server:libs:configs:account-config")
include("server:libs:configs:liquibase-config")
include("server:libs:core:autoconfigure-annotations")
include("server:libs:core:category:category-api")
include("server:libs:core:category:category-service")
include("server:libs:core:commons:commons-data")
include("server:libs:core:commons:commons-util")
include("server:libs:core:data-storage:data-storage-api")
include("server:libs:core:data-storage:data-storage-db:data-storage-db-api")
include("server:libs:core:data-storage:data-storage-db:data-storage-db-service")
include("server:libs:core:encryption:encryption-api")
include("server:libs:core:encryption:encryption-filesystem")
include("server:libs:core:encryption:encryption-impl")
include("server:libs:core:error:error-api")
include("server:libs:core:error:error-impl")
include("server:libs:core:evaluator")
include("server:libs:core:event:event-api")
include("server:libs:core:event:event-listener:event-listener-api")
include("server:libs:core:event:event-listener:event-listener-impl")
include("server:libs:core:event:event-impl")
include("server:libs:core:file-storage:file-storage-api")
include("server:libs:core:file-storage:file-storage-base64-service")
include("server:libs:core:file-storage:file-storage-filesystem-service")
include("server:libs:core:message-broker:message-broker-api")
include("server:libs:core:message-broker:message-broker-amqp")
include("server:libs:core:message-broker:message-broker-jms")
include("server:libs:core:message-broker:message-broker-kafka")
include("server:libs:core:message-broker:message-broker-redis")
include("server:libs:core:message-broker:message-broker-sync")
include("server:libs:core:rest:rest-api")
include("server:libs:core:rest:rest-impl")
include("server:libs:core:tag:tag-api")
include("server:libs:core:tag:tag-service")

include("server:libs:helios:helios-connection:helios-connection-api")
include("server:libs:helios:helios-connection:helios-connection-rest")
include("server:libs:helios:helios-connection:helios-connection-service")
include("server:libs:helios:helios-coordinator")
include("server:libs:helios:helios-configuration:helios-configuration-api")
include("server:libs:helios:helios-configuration:helios-configuration-rest:helios-configuration-rest-api")
include("server:libs:helios:helios-configuration:helios-configuration-rest:helios-configuration-rest-impl")
include("server:libs:helios:helios-configuration:helios-configuration-service")
include("server:libs:helios:helios-execution:helios-execution-api")
include("server:libs:helios:helios-execution:helios-execution-rest")
include("server:libs:helios:helios-execution:helios-execution-service")

include("server:libs:hermes:hermes-component:hermes-component-api")
include("server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-api")
include("server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-service")
include("server:libs:hermes:hermes-component:hermes-component-test-int-support")
include("server:libs:hermes:hermes-configuration:hermes-configuration-api")
include("server:libs:hermes:hermes-configuration:hermes-configuration-rest")
include("server:libs:hermes:hermes-configuration:hermes-configuration-service")
include("server:libs:hermes:hermes-connection:hermes-connection-api")
include("server:libs:hermes:hermes-connection:hermes-connection-service")
include("server:libs:hermes:hermes-coordinator:hermes-coordinator-api")
include("server:libs:hermes:hermes-coordinator:hermes-coordinator-impl")
include("server:libs:hermes:hermes-definition-api")
include("server:libs:hermes:hermes-execution:hermes-execution-api")
include("server:libs:hermes:hermes-execution:hermes-execution-service")
include("server:libs:hermes:hermes-registry-api")
include("server:libs:hermes:hermes-scheduler:hermes-scheduler-api")
include("server:libs:hermes:hermes-scheduler:hermes-scheduler-impl")
include("server:libs:hermes:hermes-task-dispatcher:hermes-task-dispatcher-api")
include("server:libs:hermes:hermes-task-dispatcher:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-api")
include("server:libs:hermes:hermes-task-dispatcher:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-service")
include("server:libs:hermes:hermes-task-dispatcher:hermes-task-dispatcher-test-int-support")
include("server:libs:hermes:hermes-test-executor:hermes-test-executor-api")
include("server:libs:hermes:hermes-test-executor:hermes-test-executor-impl")
include("server:libs:hermes:hermes-webhook:hermes-webhook-api")
include("server:libs:hermes:hermes-webhook:hermes-webhook-impl")
include("server:libs:hermes:hermes-webhook:hermes-webhook-rest")
include("server:libs:hermes:hermes-worker:hermes-worker-api")
include("server:libs:hermes:hermes-worker:hermes-worker-impl")

include("server:libs:modules:components:airtable")
include("server:libs:modules:components:aws:aws-s3")
include("server:libs:modules:components:bash")
include("server:libs:modules:components:csvfile")
include("server:libs:modules:components:datamapper")
include("server:libs:modules:components:datastorage")
include("server:libs:modules:components:delay")
include("server:libs:modules:components:email")
include("server:libs:modules:components:filestorage")
include("server:libs:modules:components:httpclient")
include("server:libs:modules:components:hubspot")
include("server:libs:modules:components:jira")
include("server:libs:modules:components:jsonfile")
include("server:libs:modules:components:filesystem")
include("server:libs:modules:components:htmlhelper")
include("server:libs:modules:components:logger")
include("server:libs:modules:components:mailchimp")
include("server:libs:modules:components:map")
include("server:libs:modules:components:mysql")
include("server:libs:modules:components:objecthelper")
include("server:libs:modules:components:odsfile")
include("server:libs:modules:components:petstore")
include("server:libs:modules:components:pipedrive")
include("server:libs:modules:components:postgresql")
include("server:libs:modules:components:randomhelper")
include("server:libs:modules:components:rabbitmq")
include("server:libs:modules:components:schedule")
include("server:libs:modules:components:script")
//include("server:libs:modules:components:shopify")
include("server:libs:modules:components:var")
include("server:libs:modules:components:xlsxfile")
include("server:libs:modules:components:xmlfile")
include("server:libs:modules:components:xmlhelper")
include("server:libs:modules:components:webhook")

include("server:libs:modules:task-dispatchers:branch")
include("server:libs:modules:task-dispatchers:condition")
include("server:libs:modules:task-dispatchers:each")
include("server:libs:modules:task-dispatchers:forkjoin")
include("server:libs:modules:task-dispatchers:loop")
include("server:libs:modules:task-dispatchers:map")
include("server:libs:modules:task-dispatchers:parallel")
include("server:libs:modules:task-dispatchers:sequence")
include("server:libs:modules:task-dispatchers:subflow")

include("server:libs:test:test-support")
include("server:libs:test:test-int-support")

include("server:ee:apps:api-gateway-service-app")
include("server:ee:apps:configuration-service-app")
include("server:ee:apps:connection-service-app")
include("server:ee:apps:coordinator-service-app")
include("server:ee:apps:execution-service-app")
include("server:ee:apps:scheduler-service-app")
include("server:ee:apps:webhook-service-app")
include("server:ee:apps:worker-service-app")

include("server:ee:libs:atlas:atlas-execution:atlas-execution-remote-rest")
include("server:ee:libs:atlas:atlas-execution:atlas-execution-remote-client")
include("server:ee:libs:core:commons:commons-discovery")
include("server:ee:libs:core:commons:commons-webclient")
include("server:ee:libs:core:data-storage:data-storage-db:data-storage-db-remote-rest")
include("server:ee:libs:core:data-storage:data-storage-db:data-storage-db-remote-client")
include("server:ee:libs:core:discovery:discovery-metadata-api")
include("server:ee:libs:core:discovery:discovery-redis")
include("server:ee:libs:dione:dione-configuration:dione-configuration-api")
include("server:ee:libs:dione:dione-configuration:dione-configuration-rest")
include("server:ee:libs:dione:dione-configuration:dione-configuration-service")
include("server:ee:libs:helios:helios-configuration:helios-configuration-remote-rest")
include("server:ee:libs:helios:helios-configuration:helios-configuration-remote-client")
include("server:ee:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-client")
include("server:ee:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-rest")
include("server:ee:libs:hermes:hermes-configuration:hermes-configuration-remote-client")
include("server:ee:libs:hermes:hermes-configuration:hermes-configuration-remote-rest")
include("server:ee:libs:hermes:hermes-connection:hermes-connection-remote-client")
include("server:ee:libs:hermes:hermes-connection:hermes-connection-remote-rest")
include("server:ee:libs:hermes:hermes-execution:hermes-execution-remote-client")
include("server:ee:libs:hermes:hermes-execution:hermes-execution-remote-rest")
include("server:ee:libs:hermes:hermes-scheduler:hermes-scheduler-remote-rest")
include("server:ee:libs:hermes:hermes-scheduler:hermes-scheduler-remote-client")
include("server:ee:libs:hermes:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-remote-client")
include("server:ee:libs:hermes:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-remote-rest")
include("server:ee:libs:zeus:zeus-execution:zeus-execution-rest")
