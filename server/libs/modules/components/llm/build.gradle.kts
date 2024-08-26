version="1.0"

configurations{
    create("test")
}

tasks.register<Jar>("testArchive"){
    archiveBaseName.set("LLM-test")
    from(project.the<SourceSetContainer>()["test"].output)
}

artifacts{
    add("test", tasks["testArchive"])
}

dependencies {
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M2")
}
