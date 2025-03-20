/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.ai.facade.dto.ContextDTO;
import com.knuddels.jtokkit.api.EncodingType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotFacadeImpl implements ChatFacade {
    private final ChatClient chatClientWorkflow;
    private final ChatClient chatClientDocs;
    private final ChatClient chatClientScript;
    private final WorkflowService workflowService;

    private static VectorStore vectorStore;

    private static final String CATEGORY = "category";
    private static final String NAME = "name";

    @SuppressFBWarnings("EI")
    @Autowired
    public AiCopilotFacadeImpl(ChatClient.Builder chatClientBuilder, WorkflowService workflowService, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // moja mala skriptica za punjenje baze, teba pokrenuti na startupu jednom
//        addDocumentsToVectorDatabase();


        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(new InMemoryChatMemory());

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder();
        QuestionAnswerAdvisor questionAnswerAdvisorDocs = new QuestionAnswerAdvisor(vectorStore, searchRequestBuilder.filterExpression("category == 'documentation'").build());
        QuestionAnswerAdvisor questionAnswerAdvisorComponents = new QuestionAnswerAdvisor(vectorStore, searchRequestBuilder.filterExpression("category == 'components'").build());
        QuestionAnswerAdvisor questionAnswerAdvisorWorkflow = new QuestionAnswerAdvisor(vectorStore, searchRequestBuilder.filterExpression("category == 'workflows'").build());

        this.chatClientDocs = chatClientBuilder
            .clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorDocs,
                questionAnswerAdvisorComponents
            )
            .build();

        this.chatClientWorkflow = chatClientBuilder
            .clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorWorkflow,
                questionAnswerAdvisorComponents
            )
            .build();

        this.chatClientScript = chatClientBuilder
            .clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents
                //add script advisor
            )
            .build();

        this.workflowService = workflowService;
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, ContextDTO contextDTO, String conversationId) {
        Workflow workflow = workflowService.getWorkflow(contextDTO.workflowId());

        final String userPrompt = """
              Current workflow:
              {workflow}
              Instructions:
              {message}
            """;
        final String workflowString = "workflow";
        final String messageString = "message";

        return switch (contextDTO.source()){
            case WORKFLOW_EDITOR -> {
                ChatClient.ChatClientRequestSpec advisors = chatClientDocs.prompt()
                    .system("You are a Bytechef assistant. You answer questions about Bytechef and help users with problems. If a user asks you about generating a workflow: answer only with a json in a format similar to the json objects in the vector database. Only use the actions, triggers and parameters which you know exist.")
                    .user(message)
                    .advisors(advisor -> advisor
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                        )
                    ; //.param("clear_memory", true).param("chat_memory_response_size", 200)
                yield advisors
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            }
            case WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU ->
                chatClientWorkflow.prompt()
                    .system("Answer only with a json in a format similar to the json objects in the vector database. Only use the actions, triggers and parameters which you know exist; look for JSON Example for the action or trigger. If a parameter is required, you must use it.") //ARRAY_BUILDER is an indicator that the parameter must be in [] brackets. OBJECT_BUILDER is an indicator that the parameter must be in {} brackets.
                    .user(u -> u.text(userPrompt)
                        .param(workflowString, workflow.getDefinition())
                        .param(messageString, message))
                    .advisors(advisor -> advisor
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            case CODE_EDITOR ->
                switch (contextDTO.parameters().get("language").toString()){
                    case "javascript" -> chatClientScript.prompt()
                        .system("You are a javascript code generator, answer only with code.")
                        .user(u -> u.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "python" -> chatClientScript.prompt()
                        .system("You are a python code generator, answer only with code.")
                        .user(u -> u.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "ruby" -> chatClientScript.prompt()
                        .system("You are a ruby code generator, answer only with code.")
                        .user(u -> u.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    default -> throw new IllegalStateException("Unexpected value: " + contextDTO.parameters().get("language").toString());
                };
            case null, default -> throw new IllegalStateException("Unexpected value: " + contextDTO.source());
        };
    }


    public static String preprocessDocument(String document) {
        Pattern htmlTagsPattern = Pattern.compile("<[^>]*>");
        document = htmlTagsPattern.matcher(document).replaceAll("");

        Pattern pixelLinkPattern = Pattern.compile("^!.+$", Pattern.MULTILINE);
        Matcher pixelLinkMatcher = pixelLinkPattern.matcher(document);
        document = pixelLinkMatcher.replaceAll("");

        Pattern colonPattern = Pattern.compile("^\\|:.+$", Pattern.MULTILINE);
        Matcher colonMatcher = colonPattern.matcher(document);
        document = colonMatcher.replaceAll("");

        Pattern linkPattern = Pattern.compile("^\\[.*\\)$", Pattern.MULTILINE);
        Matcher linkMatcher = linkPattern.matcher(document);
        document = linkMatcher.replaceAll("");

        Pattern headerPattern = Pattern.compile("^---.*\\n([\\s\\S]*?)^---\n", Pattern.MULTILINE);
        Matcher headerMatcher = headerPattern.matcher(document);
        document = headerMatcher.replaceAll("");

        //properties and tables
        Pattern propertiesPattern = Pattern.compile("^#### Properties.*$", Pattern.MULTILINE);
        Matcher propertiesMatcher = propertiesPattern.matcher(document);
        document = propertiesMatcher.replaceAll("");

        Pattern tablePattern = Pattern.compile("^\\|.*\\|$", Pattern.MULTILINE);
        Matcher tableMatcher = tablePattern.matcher(document);
        document = tableMatcher.replaceAll("");
        //

        Pattern spacePattern = Pattern.compile("\\s+");
        document = spacePattern.matcher(document).replaceAll(" ");

        return document.trim();
    }

    // Function to split a document into chunks based on a maximum token limit
    public static List<String> splitDocument(String[] tokens, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int tokenCount = 0;

        for (String token : tokens) {
            if (tokenCount + 1 > maxTokens) {
                chunks.add(currentChunk.toString().trim());
                currentChunk.setLength(0); // Reset the current chunk
                tokenCount = 0;
            }
            currentChunk.append(token).append(" ");
            tokenCount++;
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private static void storeDocumentsFromPath(String name, Path path, String suffix, BatchingStrategy batchingStrategy) throws IOException {
        List<Document> documentList = new ArrayList<>();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(suffix)) {
                    String document = Files.readString(file);
                    String fileName = file.getFileName().toString().replace(suffix, "");

                    // Preprocess the document
                    String cleanedDocument = preprocessDocument(document);

                    if(!cleanedDocument.isEmpty()) {
                        // Split the document into chunks
                        List<String> chunks = splitDocument(cleanedDocument.split("\\s+"), 1536);

                        for (String chunk : chunks) {
                            //System.out.println("Chunk " + (i + 1) + ": " + chunks.get(i));
                            documentList.add(new Document(chunk, Map.of(CATEGORY, name, NAME, fileName)));
                        }
                    }

                }
                return FileVisitResult.CONTINUE;
            }
        });

        for(List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private void addDocumentsToVectorDatabase() {
        TokenCountBatchingStrategy strategy = new TokenCountBatchingStrategy(EncodingType.CL100K_BASE, 8191, 0.1, Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);

        String docsName = "documentation";
        String workflowsName = "workflows";
        String componentsName = "components";

        Path docsDir = Paths.get("/home/user/IdeaProjects/bytechef/docs/src/content/docs/automation");
        Path componentsDir = Paths.get("/home/user/IdeaProjects/bytechef/docs/src/content/docs/reference/components");  //include task-dispatchers when documentation is written
        Path workflowsDir = Paths.get("/home/user/IdeaProjects/bytechef-workflows/projects/samples/basic_workflows");

        try {
            storeDocumentsFromPath(docsName, docsDir, ".md", strategy);
            storeDocumentsFromPath(workflowsName, workflowsDir, ".json", strategy);
            storeDocumentsFromPath(componentsName, componentsDir, ".md", strategy);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Path welomePath = Paths.get( "/home/user/IdeaProjects/bytechef/docs/src/content/docs/welcome.md");
            String welcome = Files.readString(welomePath);
            vectorStore.add(List.of(new Document(welcome, Map.of(CATEGORY, docsName, NAME, "welcome"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
