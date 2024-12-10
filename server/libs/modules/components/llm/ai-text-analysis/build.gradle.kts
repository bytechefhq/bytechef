version="1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    implementation(project(":server:libs:modules:components:llm:amazon-bedrock"))
    implementation(project(":server:libs:modules:components:llm:anthropic"))
    implementation(project(":server:libs:modules:components:llm:azure-openai"))
    implementation(project(":server:libs:modules:components:llm:groq"))
    implementation(project(":server:libs:modules:components:llm:hugging-face"))
    implementation(project(":server:libs:modules:components:llm:mistral"))
    implementation(project(":server:libs:modules:components:llm:nvidia"))
    implementation(project(":server:libs:modules:components:llm:openai"))
    implementation(project(":server:libs:modules:components:llm:stability"))
    implementation(project(":server:libs:modules:components:llm:vertex:gemini"))
    implementation(project(":server:libs:modules:components:llm:watsonx"))
}
