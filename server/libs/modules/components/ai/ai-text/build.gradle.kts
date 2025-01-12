version="1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api"))

    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:modules:components:ai:llm:amazon-bedrock"))
    implementation(project(":server:libs:modules:components:ai:llm:anthropic"))
    implementation(project(":server:libs:modules:components:ai:llm:azure-openai"))
    implementation(project(":server:libs:modules:components:ai:llm:groq"))
    implementation(project(":server:libs:modules:components:ai:llm:hugging-face"))
    implementation(project(":server:libs:modules:components:ai:llm:mistral"))
    implementation(project(":server:libs:modules:components:ai:llm:nvidia"))
    implementation(project(":server:libs:modules:components:ai:llm:openai"))
    implementation(project(":server:libs:modules:components:ai:llm:stability"))
    implementation(project(":server:libs:modules:components:ai:llm:vertex:gemini"))
    implementation(project(":server:libs:modules:components:ai:llm:watsonx"))
}
