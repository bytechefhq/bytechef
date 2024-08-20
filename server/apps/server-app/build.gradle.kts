group = "com.bytechef.server"
description = "ByteChef server app"

springBoot {
    mainClass.set("com.bytechef.server.ServerApplication")
}

dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation(libs.org.springdoc.springdoc.openapi.starter.common)
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-config"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-service"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-config"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-config"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-impl"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-config"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-instance-impl"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    implementation(project(":server:libs:automation:automation-connection:automation-connection-rest"))
    implementation(project(":server:libs:automation:automation-connection:automation-connection-service"))
    implementation(project(":server:libs:automation:automation-swagger"))
    implementation(project(":server:libs:automation:automation-user:automation-user-rest"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-coordinator"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-rest"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:automation-demo-config"))
    implementation(project(":server:libs:config:cache-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:jdbc-config"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:config:rest-config"))
    implementation(project(":server:libs:config:security-config"))
    implementation(project(":server:libs:config:static-resources-config"))
    implementation(project(":server:libs:config:tenant-single-security-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:encryption:encryption-filesystem"))
    implementation(project(":server:libs:core:encryption:encryption-impl"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-noop-service"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-jms"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:core:tenant:tenant-single-service"))
    implementation(project(":server:libs:embedded:embedded-connected-user:embedded-connected-user-rest"))
    implementation(project(":server:libs:embedded:embedded-connected-user:embedded-connected-user-service"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-connected-user-token-rest"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-instance-impl"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-public-rest:embedded-configuration-public-rest-impl"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-service"))
    implementation(project(":server:libs:embedded:embedded-connection:embedded-connection-rest"))
    implementation(project(":server:libs:embedded:embedded-connectivity:embedded-connectivity-public-rest"))
    implementation(project(":server:libs:embedded:embedded-connectivity:embedded-connectivity-service"))
    implementation(project(":server:libs:embedded:embedded-security-web:embedded-security-web-impl"))
    implementation(project(":server:libs:embedded:embedded-swagger"))
    implementation(project(":server:libs:embedded:embedded-user:embedded-user-rest"))
    implementation(project(":server:libs:embedded:embedded-workflow:embedded-workflow-coordinator"))
    implementation(project(":server:libs:embedded:embedded-workflow:embedded-workflow-execution:embedded-workflow-execution-rest"))
    implementation(project(":server:libs:embedded:embedded-workflow:embedded-workflow-execution:embedded-workflow-execution-service"))
    implementation(project(":server:libs:platform:platform-category:platform-category-service"))
    implementation(project(":server:libs:platform:platform-component:platform-component-registry:platform-component-registry-service"))
    implementation(project(":server:libs:platform:platform-component:platform-component-rest"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-service"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-file-storage:platform-data-storage-file-storage-service"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-service"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-impl"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-service"))
    implementation(project(":server:libs:platform:platform-rest:platform-rest-impl"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-impl"))
    implementation(project(":server:libs:platform:platform-security:platform-security-web:platform-security-web-impl"))
    implementation(project(":server:libs:platform:platform-swagger"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    implementation(project(":server:libs:platform:platform-user:platform-user-rest:platform-user-rest-impl"))
    implementation(project(":server:libs:platform:platform-user:platform-user-service"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-impl"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-coordinator:platform-workflow-coordinator-impl"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-registry:platform-workflow-task-dispatcher-registry-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-rest"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-rest:platform-workflow-execution-rest-impl"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-impl"))

    implementation(project(":server:libs:modules:components:accelo"))
    implementation(project(":server:libs:modules:components:active-campaign"))
    implementation(project(":server:libs:modules:components:affinity"))
    implementation(project(":server:libs:modules:components:airtable"))
    implementation(project(":server:libs:modules:components:aitable"))
    implementation(project(":server:libs:modules:components:asana"))
    implementation(project(":server:libs:modules:components:aws:aws-s3"))
    implementation(project(":server:libs:modules:components:bash"))
    implementation(project(":server:libs:modules:components:box"))
    implementation(project(":server:libs:modules:components:capsule-crm"))
    implementation(project(":server:libs:modules:components:clickup"))
    implementation(project(":server:libs:modules:components:copper"))
    implementation(project(":server:libs:modules:components:csv-file"))
    implementation(project(":server:libs:modules:components:data-mapper"))
    implementation(project(":server:libs:modules:components:data-storage"))
    implementation(project(":server:libs:modules:components:data-stream"))
    implementation(project(":server:libs:modules:components:date-helper"))
    implementation(project(":server:libs:modules:components:delay"))
    implementation(project(":server:libs:modules:components:discord"))
    implementation(project(":server:libs:modules:components:dropbox"))
    implementation(project(":server:libs:modules:components:email"))
    implementation(project(":server:libs:modules:components:encharge"))
    implementation(project(":server:libs:modules:components:file-storage"))
    implementation(project(":server:libs:modules:components:filesystem"))
    implementation(project(":server:libs:modules:components:freshdesk"))
    implementation(project(":server:libs:modules:components:freshsales"))
    implementation(project(":server:libs:modules:components:github"))
    implementation(project(":server:libs:modules:components:gitlab"))
    implementation(project(":server:libs:modules:components:google:google-calendar"))
    implementation(project(":server:libs:modules:components:google:google-contacts"))
    implementation(project(":server:libs:modules:components:google:google-docs"))
    implementation(project(":server:libs:modules:components:google:google-drive"))
    implementation(project(":server:libs:modules:components:google:google-mail"))
    implementation(project(":server:libs:modules:components:google:google-sheets"))
    implementation(project(":server:libs:modules:components:http-client"))
    implementation(project(":server:libs:modules:components:hubspot"))
    implementation(project(":server:libs:modules:components:infobip"))
    implementation(project(":server:libs:modules:components:insightly"))
    implementation(project(":server:libs:modules:components:intercom"))
    implementation(project(":server:libs:modules:components:jira"))
    implementation(project(":server:libs:modules:components:json-file"))
    implementation(project(":server:libs:modules:components:keap"))
    implementation(project(":server:libs:modules:components:logger"))
    implementation(project(":server:libs:modules:components:mailchimp"))
    implementation(project(":server:libs:modules:components:map"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-excel"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-one-drive"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-outlook-365"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-share-point"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-teams"))
    implementation(project(":server:libs:modules:components:monday"))
    implementation(project(":server:libs:modules:components:myob"))
    implementation(project(":server:libs:modules:components:mysql"))
    implementation(project(":server:libs:modules:components:nifty"))
    implementation(project(":server:libs:modules:components:object-helper"))
    implementation(project(":server:libs:modules:components:ods-file"))
    implementation(project(":server:libs:modules:components:one-simple-api"))
    implementation(project(":server:libs:modules:components:llm:amazon-bedrock"))
    implementation(project(":server:libs:modules:components:llm:anthropic"))
    implementation(project(":server:libs:modules:components:llm:azure-openai"))
    implementation(project(":server:libs:modules:components:llm:groq"))
    implementation(project(":server:libs:modules:components:llm:hugging-face"))
    implementation(project(":server:libs:modules:components:llm:minimax"))
    implementation(project(":server:libs:modules:components:llm:mistral"))
    implementation(project(":server:libs:modules:components:llm:moonshot"))
    implementation(project(":server:libs:modules:components:llm:nvidia"))
    implementation(project(":server:libs:modules:components:llm:ollama"))
    implementation(project(":server:libs:modules:components:llm:openai"))
    implementation(project(":server:libs:modules:components:llm:qianfan"))
    implementation(project(":server:libs:modules:components:llm:vertex:gemini"))
    implementation(project(":server:libs:modules:components:llm:vertex:palm2"))
    implementation(project(":server:libs:modules:components:llm:watsonx"))
    implementation(project(":server:libs:modules:components:llm:zhipu"))
    implementation(project(":server:libs:modules:components:petstore"))
    implementation(project(":server:libs:modules:components:pipedrive"))
    implementation(project(":server:libs:modules:components:pipeliner"))
    implementation(project(":server:libs:modules:components:postgresql"))
    implementation(project(":server:libs:modules:components:quickbooks"))
    implementation(project(":server:libs:modules:components:rabbitmq"))
    implementation(project(":server:libs:modules:components:random-helper"))
    implementation(project(":server:libs:modules:components:reckon"))
    implementation(project(":server:libs:modules:components:resend"))
    implementation(project(":server:libs:modules:components:salesflare"))
    implementation(project(":server:libs:modules:components:schedule"))
    implementation(project(":server:libs:modules:components:script"))
    implementation(project(":server:libs:modules:components:sendgrid"))
    implementation(project(":server:libs:modules:components:shopify"))
    implementation(project(":server:libs:modules:components:slack"))
    implementation(project(":server:libs:modules:components:spotify"))
    implementation(project(":server:libs:modules:components:teamwork"))
    implementation(project(":server:libs:modules:components:text-helper"))
    implementation(project(":server:libs:modules:components:todoist"))
    implementation(project(":server:libs:modules:components:trello"))
    implementation(project(":server:libs:modules:components:twilio"))
    implementation(project(":server:libs:modules:components:typeform"))
    implementation(project(":server:libs:modules:components:var"))
    implementation(project(":server:libs:modules:components:vtiger"))
    implementation(project(":server:libs:modules:components:webhook"))
    implementation(project(":server:libs:modules:components:whatsapp"))
    implementation(project(":server:libs:modules:components:xero"))
    implementation(project(":server:libs:modules:components:xlsx-file"))
    implementation(project(":server:libs:modules:components:xml-file"))
    implementation(project(":server:libs:modules:components:xml-helper"))
    implementation(project(":server:libs:modules:components:zendesk-sell"))
    implementation(project(":server:libs:modules:components:zoho:zoho-crm"))

    implementation(project(":server:libs:modules:task-dispatchers:branch"))
    implementation(project(":server:libs:modules:task-dispatchers:condition"))
    implementation(project(":server:libs:modules:task-dispatchers:each"))
    implementation(project(":server:libs:modules:task-dispatchers:fork-join"))
    implementation(project(":server:libs:modules:task-dispatchers:loop"))
    implementation(project(":server:libs:modules:task-dispatchers:map"))
    implementation(project(":server:libs:modules:task-dispatchers:parallel"))
    implementation(project(":server:libs:modules:task-dispatchers:subflow"))

    implementation(project(":server:ee:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-git"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-rest"))
    implementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-message-event-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-security-config"))
    implementation(project(":server:ee:libs:core:audit:audit-service"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))
    implementation(project(":server:ee:libs:core:encryption:encryption-aws"))
    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws"))
    implementation(project(":server:ee:libs:core:message:message-broker:message-broker-aws"))
    implementation(project(":server:ee:libs:core:tenant:tenant-multi-service"))

    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.apache.activemq:artemis-jakarta-server")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.org.springdoc.springdoc.openapi.starter.webmvc.ui)
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-amqp")
    runtimeOnly("org.springframework.boot:spring-boot-starter-artemis")
    runtimeOnly("org.springframework.boot:spring-boot-starter-cache")
    runtimeOnly("org.springframework.boot:spring-boot-starter-quartz")
    runtimeOnly("org.springframework.boot:spring-boot-starter-mail")
    runtimeOnly("org.springframework.kafka:spring-kafka")

    testImplementation(project(":server:libs:test:test-int-support"))
}
