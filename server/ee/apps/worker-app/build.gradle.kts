group = "com.bytechef.worker"
description = ""

springBoot {
    mainClass.set("com.bytechef.worker.WorkerApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.loki.logback.appender)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-impl"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-config"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-file-storage:platform-data-storage-file-storage-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-impl"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-impl"))

    implementation(project(":server:ee:libs:config:tenant-multi-message-event-config"))
    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:core:remote:remote-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-component:platform-component-remote-rest"))
    implementation(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-aws"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-remote-rest"))

    implementation(project(":server:libs:modules:components:accelo"))
    implementation(project(":server:libs:modules:components:active-campaign"))
    implementation(project(":server:libs:modules:components:affinity"))
    implementation(project(":server:libs:modules:components:ai:ai-agent"))
    implementation(project(":server:libs:modules:components:ai:ai-text"))
    implementation(project(":server:libs:modules:components:ai:llm:amazon-bedrock"))
    implementation(project(":server:libs:modules:components:ai:llm:anthropic"))
    implementation(project(":server:libs:modules:components:ai:llm:azure-openai"))
    implementation(project(":server:libs:modules:components:ai:llm:groq"))
    implementation(project(":server:libs:modules:components:ai:llm:hugging-face"))
    implementation(project(":server:libs:modules:components:ai:llm:mistral"))
    implementation(project(":server:libs:modules:components:ai:llm:nvidia"))
    implementation(project(":server:libs:modules:components:ai:llm:ollama"))
    implementation(project(":server:libs:modules:components:ai:llm:openai"))
    implementation(project(":server:libs:modules:components:ai:llm:stability"))
    implementation(project(":server:libs:modules:components:ai:llm:vertex:gemini"))
    implementation(project(":server:libs:modules:components:ai:llm:watsonx"))
    implementation(project(":server:libs:modules:components:ai:vector-store:pinecone"))
    implementation(project(":server:libs:modules:components:ai:vector-store:weaviate"))
    implementation(project(":server:libs:modules:components:airtable"))
    implementation(project(":server:libs:modules:components:aitable"))
    implementation(project(":server:libs:modules:components:app-event"))
    implementation(project(":server:libs:modules:components:asana"))
    implementation(project(":server:libs:modules:components:aws:aws-s3"))
    implementation(project(":server:libs:modules:components:bash"))
    implementation(project(":server:libs:modules:components:baserow"))
    implementation(project(":server:libs:modules:components:box"))
    implementation(project(":server:libs:modules:components:capsule-crm"))
    implementation(project(":server:libs:modules:components:clickup"))
    implementation(project(":server:libs:modules:components:copper"))
    implementation(project(":server:libs:modules:components:csv-file"))
    implementation(project(":server:libs:modules:components:data-mapper"))
    implementation(project(":server:libs:modules:components:data-storage"))
    implementation(project(":server:libs:modules:components:date-helper"))
    implementation(project(":server:libs:modules:components:delay"))
    implementation(project(":server:libs:modules:components:discord"))
    implementation(project(":server:libs:modules:components:dropbox"))
    implementation(project(":server:libs:modules:components:email"))
    implementation(project(":server:libs:modules:components:encharge"))
    implementation(project(":server:libs:modules:components:figma"))
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
    implementation(project(":server:libs:modules:components:google:google-forms"))
    implementation(project(":server:libs:modules:components:google:google-mail"))
    implementation(project(":server:libs:modules:components:google:google-sheets"))
    implementation(project(":server:libs:modules:components:http-client"))
    implementation(project(":server:libs:modules:components:hubspot"))
    implementation(project(":server:libs:modules:components:image-helper"))
    implementation(project(":server:libs:modules:components:infobip"))
    implementation(project(":server:libs:modules:components:insightly"))
    implementation(project(":server:libs:modules:components:intercom"))
    implementation(project(":server:libs:modules:components:jira"))
    implementation(project(":server:libs:modules:components:jotform"))
    implementation(project(":server:libs:modules:components:json-file"))
    implementation(project(":server:libs:modules:components:json-helper"))
    implementation(project(":server:libs:modules:components:keap"))
    implementation(project(":server:libs:modules:components:logger"))
    implementation(project(":server:libs:modules:components:mailchimp"))
    implementation(project(":server:libs:modules:components:map"))
    implementation(project(":server:libs:modules:components:math-helper"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-excel"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-one-drive"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-outlook-365"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-share-point"))
    implementation(project(":server:libs:modules:components:microsoft:microsoft-teams"))
    implementation(project(":server:libs:modules:components:monday"))
    implementation(project(":server:libs:modules:components:myob"))
    implementation(project(":server:libs:modules:components:mysql"))
    implementation(project(":server:libs:modules:components:nifty"))
    implementation(project(":server:libs:modules:components:nutshell"))
    implementation(project(":server:libs:modules:components:object-helper"))
    implementation(project(":server:libs:modules:components:ods-file"))
    implementation(project(":server:libs:modules:components:one-simple-api"))
    implementation(project(":server:libs:modules:components:petstore"))
    implementation(project(":server:libs:modules:components:pipedrive"))
    implementation(project(":server:libs:modules:components:pipeliner"))
    implementation(project(":server:libs:modules:components:postgresql"))
    implementation(project(":server:libs:modules:components:quickbooks"))
    implementation(project(":server:libs:modules:components:rabbitmq"))
    implementation(project(":server:libs:modules:components:random-helper"))
    implementation(project(":server:libs:modules:components:reckon"))
    implementation(project(":server:libs:modules:components:resend"))
    implementation(project(":server:libs:modules:components:request"))
    implementation(project(":server:libs:modules:components:salesflare"))
    implementation(project(":server:libs:modules:components:schedule"))
    implementation(project(":server:libs:modules:components:script"))
    implementation(project(":server:libs:modules:components:sendgrid"))
    implementation(project(":server:libs:modules:components:shopify"))
    implementation(project(":server:libs:modules:components:slack"))
    implementation(project(":server:libs:modules:components:spotify"))
    implementation(project(":server:libs:modules:components:stripe"))
    implementation(project(":server:libs:modules:components:teamwork"))
    implementation(project(":server:libs:modules:components:text-helper"))
    implementation(project(":server:libs:modules:components:todoist"))
    implementation(project(":server:libs:modules:components:trello"))
    implementation(project(":server:libs:modules:components:twilio"))
    implementation(project(":server:libs:modules:components:typeform"))
    implementation(project(":server:libs:modules:components:var"))
    implementation(project(":server:libs:modules:components:vtiger"))
    implementation(project(":server:libs:modules:components:webflow"))
    implementation(project(":server:libs:modules:components:webhook"))
    implementation(project(":server:libs:modules:components:whatsapp"))
    implementation(project(":server:libs:modules:components:xero"))
    implementation(project(":server:libs:modules:components:xlsx-file"))
    implementation(project(":server:libs:modules:components:xml-file"))
    implementation(project(":server:libs:modules:components:xml-helper"))
    implementation(project(":server:libs:modules:components:zendesk-sell"))
    implementation(project(":server:libs:modules:components:zeplin"))
    implementation(project(":server:libs:modules:components:zoho:zoho-crm"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-amqp")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.kafka:spring-kafka")

    testImplementation(project(":server:libs:test:test-int-support"))
}
