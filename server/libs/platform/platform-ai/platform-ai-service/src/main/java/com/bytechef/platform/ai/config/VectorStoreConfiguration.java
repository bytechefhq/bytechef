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

package com.bytechef.platform.ai.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.service.VectorStoreService;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class VectorStoreConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(VectorStoreConfiguration.class);

    private static final String CATEGORY = "category";
    private static final String NAME = "name";
    private static final String DOCUMENTATION = "documentation";
    private static final String WORKFLOWS = "workflows";
    private static final String COMPONENTS = "components";
    private static final String FLOWS = "flows";

    private final ApplicationProperties.Ai.Paths paths;
    private final TokenCountBatchingStrategy strategy;
    private final VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;
    private final ComponentDefinitionService componentDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @SuppressFBWarnings("EI")
    @Autowired
    public VectorStoreConfiguration(
        VectorStore vectorStore, VectorStoreService vectorStoreService,
        ComponentDefinitionService componentDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService, ApplicationProperties applicationProperties) {

        this.paths = applicationProperties.getAi()
            .getPaths();
        this.vectorStore = vectorStore;
        this.vectorStoreService = vectorStoreService;
        this.strategy = new TokenCountBatchingStrategy(
            EncodingType.CL100K_BASE, 8191, 0.1, Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);
        this.componentDefinitionService = componentDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStartedEvent() {
        if (paths.getWelcomePath() == null || paths.getDocumentationPath() == null ||
            paths.getComponentsPath() == null || paths.getWorkflowsPath() == null) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Documentation paths not set.");
            }

            return;
        }

        initializeVectorStoreTable(
            Paths.get(paths.getDocumentationPath()),
            Paths.get(paths.getWelcomePath()),
            Paths.get(paths.getWorkflowsPath()));
    }

    private void initializeVectorStoreTable(
        Path documentationPath, Path welcomePath, Path workflowsPath) {

        if (vectorStoreService.count() > 0) {
            List<com.bytechef.platform.ai.domain.VectorStore> vectorsStores = vectorStoreService.findAll();
            List<Map<String, Object>> vectorsMetadataList = vectorsStores.stream()
                .map(com.bytechef.platform.ai.domain.VectorStore::getMetadata)
                .toList();

            storeDocuments(vectorsMetadataList, documentationPath, welcomePath, workflowsPath);
        } else {
            storeDocuments(List.of(), documentationPath, welcomePath, workflowsPath);
        }
    }

    private static String preprocessDocument(String document) {
        Pattern htmlTagsPattern = Pattern.compile("<[^>]*>");

        Matcher matcher = htmlTagsPattern.matcher(document);

        document = matcher.replaceAll("");

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

        // properties and tables
        Pattern propertiesPattern = Pattern.compile("^#### Properties.*$", Pattern.MULTILINE);

        Matcher propertiesMatcher = propertiesPattern.matcher(document);

        document = propertiesMatcher.replaceAll("");

        Pattern tablePattern = Pattern.compile("^\\|.*\\|$", Pattern.MULTILINE);

        Matcher tableMatcher = tablePattern.matcher(document);

        document = tableMatcher.replaceAll("");

        Pattern spacePattern = Pattern.compile("\\s+");

        Matcher spaceMatcher = spacePattern.matcher(document);

        document = spaceMatcher.replaceAll(" ");

        return document.trim();
    }

    // Function to split a document into chunks based on a maximum token limit
    private static List<String> splitDocument(String[] tokens, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int tokenCount = 0;

        for (String token : tokens) {
            if (tokenCount + 1 > maxTokens) {
                chunks.add(StringUtils.trim(currentChunk.toString()));

                currentChunk.setLength(0); // Reset the current chunk
                tokenCount = 0;
            }

            currentChunk.append(token);
            currentChunk.append(" ");

            tokenCount++;
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(StringUtils.trim(currentChunk.toString()));
        }

        return chunks;
    }

    private String getSampleValue(PropertyDecorator property) {
        return switch (property.getType()) {
            case ARRAY -> getArrayParameters(property.getItems());
            case BOOLEAN -> "false";
            case DATE -> "\"1980-01-01\"";
            case DATE_TIME -> "\"1980-01-01T00:00:00\"";
            case DYNAMIC_PROPERTIES -> "{}";
            case INTEGER -> "1";
            case NUMBER -> "0.0";
            case OBJECT -> getObjectParameters(property.getObjectProperties());
            case FILE_ENTRY -> getObjectParameters(property.getFileEntryProperties());
            case TIME -> "\"00:00:00\"";
            default -> "\"\"";
        };
    }

    private String createJsonExample(ActionDefinition actionDefinition) {
        StringBuilder json = new StringBuilder();

        json.append("{")
            .append("\n")
            .append("\"label\": \"")
            .append(actionDefinition.getTitle())
            .append("\",")
            .append("\n")
            .append("\"name\": \"")
            .append(actionDefinition.getName())
            .append("\",")
            .append("\n")
            .append("\"type\": \"")
            .append(actionDefinition.getComponentName())
            .append("/v")
            .append(actionDefinition.getComponentVersion())
            .append("/")
            .append(actionDefinition.getName())
            .append("\",\n");

        List<PropertyDecorator> properties =
            PropertyDecorator.toPropertyDecoratorList(actionDefinition.getProperties());
        if (!properties.isEmpty()) {
            json.append("\"parameters\": ")
                .append(getObjectParameters(properties));
        }
        return json.append("}")
            .toString();
    }

    private String createJsonExample(TriggerDefinition triggerDefinition) {
        StringBuilder json = new StringBuilder();

        json.append("{")
            .append("\n")
            .append("\"label\": \"")
            .append(triggerDefinition.getTitle())
            .append("\",")
            .append("\n")
            .append("\"name\": \"")
            .append(triggerDefinition.getName())
            .append("\",")
            .append("\n")
            .append("\"type\": \"")
            .append(triggerDefinition.getComponentName())
            .append("/v")
            .append(triggerDefinition.getComponentVersion())
            .append("/")
            .append(triggerDefinition.getName())
            .append("\",\n");

        List<PropertyDecorator> properties =
            PropertyDecorator.toPropertyDecoratorList(triggerDefinition.getProperties());
        if (!properties.isEmpty()) {
            json.append("\"parameters\": ")
                .append(getObjectParameters(properties));
        }
        return json.append("}")
            .toString();
    }

    private String createJsonExample(TaskDispatcherDefinition taskDispatcherDefinition) {
        StringBuilder json = new StringBuilder();

        json.append("{")
            .append("\n")
            .append("\"label\": \"")
            .append(taskDispatcherDefinition.getTitle())
            .append("\",")
            .append("\n")
            .append("\"name\": \"")
            .append(taskDispatcherDefinition.getName())
            .append("\",")
            .append("\n")
            .append("\"type\": \"")
            .append(taskDispatcherDefinition.getName())
            .append("/v")
            .append(taskDispatcherDefinition.getVersion())
            .append("/")
            .append("\",\n");

        List<PropertyDecorator> properties =
            PropertyDecorator.toPropertyDecoratorList(taskDispatcherDefinition.getProperties());
        if (!properties.isEmpty()) {
            json.append("\"parameters\": ")
                .append(getObjectParameters(properties));
        }
        return json.append("}")
            .toString();
    }

    private String getObjectParameters(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("{")
            .append("\n");
        for (var property : properties) {
            parameters.append("\"")
                .append(property.getName())
                .append("\": ")
                .append(getSampleValue(property))
                .append(",\n");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        return parameters.append("}")
            .toString();
    }

    private String getArrayParameters(List<PropertyDecorator> properties) {
        StringBuilder parameters = new StringBuilder();

        parameters.append("[\n");
        for (var property : properties) {
            parameters.append(getSampleValue(property))
                .append(",\n");
        }

        if (parameters.length() > 2) {
            parameters.setLength(parameters.length() - 2);
        }

        return parameters.append("]")
            .toString();
    }

    private String componentDefinitionToString(ComponentDefinition componentDefinition) {
        StringBuilder definitionText = new StringBuilder();

        definitionText.append("Component Name: ")
            .append(componentDefinition.getName())
            .append("\n")
            .append("Description: ")
            .append(componentDefinition.getDescription())
            .append("\n");

        if (!componentDefinition.getTriggers()
            .isEmpty()) {
            definitionText.append("Triggers:\n");
            for (TriggerDefinition triggerDefinition : componentDefinition.getTriggers()) {
                definitionText.append("Trigger Name: ")
                    .append(triggerDefinition.getName())
                    .append("\n")
                    .append("Description: ")
                    .append(triggerDefinition.getDescription())
                    .append("\n")
                    .append("Example JSON Structure: \n")
                    .append(createJsonExample(triggerDefinition))
                    .append("\n");
            }
        }

        if (!componentDefinition.getActions()
            .isEmpty()) {
            definitionText.append("Actions:\n");
            for (ActionDefinition actionDefinition : componentDefinition.getActions()) {
                if (!actionDefinition.getName()
                    .equals("customAction")) {
                    definitionText.append("Action Name: ")
                        .append(actionDefinition.getName())
                        .append("\n")
                        .append("Description: ")
                        .append(actionDefinition.getDescription())
                        .append("\n")
                        .append("Example JSON Structure: \n")
                        .append(createJsonExample(actionDefinition))
                        .append("\n");

                    OutputResponse outputResponse = actionDefinition.getOutputResponse();
                    if (actionDefinition.isOutputDefined() && outputResponse != null) {
                        definitionText.append("Output JSON: \n")
                            .append(getSampleValue(new PropertyDecorator(outputResponse.outputSchema())))
                            .append("\n");
                    }
                }
            }
        }

        return definitionText.toString();
    }

    private String taskDispatcherDefinitionToString(TaskDispatcherDefinition taskDispatcherDefinition) {
        StringBuilder definitionText = new StringBuilder();

        definitionText.append("Task Dispatcher Name: ")
            .append(taskDispatcherDefinition.getName())
            .append("\n")
            .append("Description: ")
            .append(taskDispatcherDefinition.getDescription())
            .append("\n")
            .append("Example JSON Structure: \n")
            .append(createJsonExample(taskDispatcherDefinition))
            .append("\n");

        OutputResponse outputResponse = taskDispatcherDefinition.getOutputResponse();
        if (taskDispatcherDefinition.isOutputDefined() && outputResponse != null) {
            definitionText.append("Output JSON: \n")
                .append(getSampleValue(new PropertyDecorator(outputResponse.outputSchema())))
                .append("\n");
        }

        return definitionText.toString();
    }

    private void storeDocumentsFromComponentDefinitions(
        TokenCountBatchingStrategy batchingStrategy,
        List<Map<String, Object>> vectorStoreList, VectorStore vectorStore) {
        List<Document> documentList = new ArrayList<>();

        for (TaskDispatcherDefinition taskDispatcherDefinition : taskDispatcherDefinitionService
            .getTaskDispatcherDefinitions()) {
            if (!taskDispatcherDefinition.getName()
                .equals("waitForApproval")) {
                String json = taskDispatcherDefinitionToString(taskDispatcherDefinition);

                addToDocumentList(vectorStoreList, taskDispatcherDefinition.getName(), json, documentList, FLOWS);
            }
        }

        for (ComponentDefinition componentDefinition : componentDefinitionService.getComponentDefinitions()) {
            String json = componentDefinitionToString(componentDefinition);

            addToDocumentList(vectorStoreList, componentDefinition.getName(), json, documentList, COMPONENTS);
        }

        for (List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private static void addToDocumentList(
        List<Map<String, Object>> vectorStoreList, String name, String json, List<Document> documentList,
        String category) {
        if (!vectorStoreListContainsFile(vectorStoreList, name, category)) {
            String cleanedDocument = preprocessDocument(json);

            if (!cleanedDocument.isEmpty()) {
                // Split the document into chunks
                List<String> chunks = splitDocument(cleanedDocument.split("\\s+"), 1536);

                for (String chunk : chunks) {
                    documentList.add(new Document(chunk, Map.of(CATEGORY, category, NAME, name)));
                }
            }
        }
    }

    private static void storeDocumentsFromPath(
        String categoryName, Path path, String suffix, BatchingStrategy batchingStrategy,
        List<Map<String, Object>> vectorStoreList, VectorStore vectorStore) throws IOException {

        List<Document> documentList = new ArrayList<>();

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            @SuppressFBWarnings("NP")
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                if (StringUtils.endsWith(filePath.toString(), suffix)) {
                    Path fileNamePath = filePath.getFileName();

                    String fileName = fileNamePath.toString();

                    fileName = fileName.replace(suffix, "");

                    String document = Files.readString(filePath);

                    addToDocumentList(vectorStoreList, fileName, document, documentList, categoryName);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        for (List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private static boolean vectorStoreListContainsFile(
        List<Map<String, Object>> vectorStoreList, String fileName, String categoryName) {

        return !vectorStoreList.isEmpty() && vectorStoreList.stream()
            .anyMatch(map -> fileName.equals(map.get(NAME)) && categoryName.equals(map.get(CATEGORY)));
    }

    private void storeDocuments(
        List<Map<String, Object>> vectorStoreList, Path documentationPath, Path welcomePath,
        Path workflowsPath) {

        try {
            storeDocumentsFromPath(DOCUMENTATION, documentationPath, ".md", strategy, vectorStoreList, vectorStore);
            storeDocumentsFromPath(WORKFLOWS, workflowsPath, ".json", strategy, vectorStoreList, vectorStore);
            storeDocumentsFromComponentDefinitions(strategy, vectorStoreList, vectorStore);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            String welcome = Files.readString(welcomePath);

            String cleanedDocument = preprocessDocument(welcome);

            vectorStore.add(List.of(new Document(cleanedDocument, Map.of(CATEGORY, DOCUMENTATION, NAME, "welcome"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
