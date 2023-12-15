group = "com.bytechef.worker"
description = ""

springBoot {
    mainClass.set("com.bytechef.worker.WorkerApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-service"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-config"))
    implementation(project(":server:libs:core:async-config"))
    implementation(project(":server:libs:core:environment-config"))
    implementation(project(":server:libs:core:jackson-config"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:rest:rest-impl"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-api"))
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-service"))
    implementation(project(":server:libs:hermes:hermes-file-storage:hermes-file-storage-service"))
    implementation(project(":server:libs:hermes:hermes-worker:hermes-worker-impl"))

    implementation(project(":ee:server:libs:core:discovery:discovery-metadata-api"))
    implementation(project(":ee:server:libs:core:discovery:discovery-redis"))
    implementation(project(":ee:server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-client"))
    implementation(project(":ee:server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-rest"))
    implementation(project(":ee:server:libs:hermes:hermes-connection:hermes-connection-remote-client"))
    implementation(project(":ee:server:libs:core:data-storage:data-storage-db:data-storage-db-remote-client"))
    implementation(project(":ee:server:libs:hermes:hermes-scheduler:hermes-scheduler-remote-client"))

    implementation(project(":server:libs:modules:components:airtable"))
    implementation(project(":server:libs:modules:components:aws:aws-s3"))
    implementation(project(":server:libs:modules:components:bash"))
    implementation(project(":server:libs:modules:components:csv-file"))
    implementation(project(":server:libs:modules:components:data-mapper"))
    implementation(project(":server:libs:modules:components:data-storage"))
    implementation(project(":server:libs:modules:components:delay"))
    implementation(project(":server:libs:modules:components:dropbox"))
    implementation(project(":server:libs:modules:components:file-storage"))
    implementation(project(":server:libs:modules:components:email"))
    implementation(project(":server:libs:modules:components:example"))
    implementation(project(":server:libs:modules:components:html-helper"))
    implementation(project(":server:libs:modules:components:http-client"))
    implementation(project(":server:libs:modules:components:hubspot"))
    implementation(project(":server:libs:modules:components:jira"))
    implementation(project(":server:libs:modules:components:json-file"))
    implementation(project(":server:libs:modules:components:logger"))
    implementation(project(":server:libs:modules:components:filesystem"))
    implementation(project(":server:libs:modules:components:google:google-drive"))
    implementation(project(":server:libs:modules:components:map"))
    implementation(project(":server:libs:modules:components:mailchimp"))
    implementation(project(":server:libs:modules:components:mysql"))
    implementation(project(":server:libs:modules:components:object-helper"))
    implementation(project(":server:libs:modules:components:ods-file"))
    implementation(project(":server:libs:modules:components:petstore"))
    implementation(project(":server:libs:modules:components:pipedrive"))
    implementation(project(":server:libs:modules:components:postgresql"))
    implementation(project(":server:libs:modules:components:quickbooks"))
    implementation(project(":server:libs:modules:components:rabbitmq"))
    implementation(project(":server:libs:modules:components:random-helper"))
    implementation(project(":server:libs:modules:components:schedule"))
    implementation(project(":server:libs:modules:components:script"))
//    implementation(project(":server:libs:modules:components:shopify"))
    implementation(project(":server:libs:modules:components:var"))
    implementation(project(":server:libs:modules:components:xlsx-file"))
    implementation(project(":server:libs:modules:components:xml-file"))
    implementation(project(":server:libs:modules:components:xml-helper"))
    implementation(project(":server:libs:modules:components:webhook"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-amqp")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.kafka:spring-kafka")

    testImplementation(project(":server:libs:test:test-int-support"))
}
