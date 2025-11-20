/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.component.definition.Property.Type;
import com.bytechef.ee.ai.copilot.service.VectorStoreService;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class VectorStoreLoaderConfiguration {

    private static final String CATEGORY = "category";
    private static final String COMPONENTS = "components";
    private static final int MAX_TOKENS = 1536;
    private static final String NAME = "name";
    private static final String COMPONENT_NAME = "componentName";
    private static final String TYPE = "type";
    private static final String WORKFLOWS = "workflows";

    private final TokenCountBatchingStrategy strategy;
    private final VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;
    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public VectorStoreLoaderConfiguration(
        VectorStore vectorStore, VectorStoreService vectorStoreService,
        // TODO Add dependency on ComponentDefinitionService, implement local ComponentDefinitionRegistry, that will
        // read generated component json definitions
        @Autowired(required = false) ComponentDefinitionService componentDefinitionService) {

        this.vectorStore = vectorStore;
        this.vectorStoreService = vectorStoreService;
        this.strategy = new TokenCountBatchingStrategy(
            EncodingType.CL100K_BASE, 8191, 0.1, Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);
        this.componentDefinitionService = componentDefinitionService;
    }

    // TODO Enable vector store initialization on startup
    // @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStartedEvent() {
        Resource resource = new ClassPathResource(WORKFLOWS);

        try {
            if (resource.exists() && resource.isFile()) {
                Path resourcePath = Paths.get(resource.getURI());

                initializeVectorStoreTable(resourcePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addToDocuments(
        List<Map<String, Object>> vectorStores, String name, String json, List<Document> documents) {

        if (!containsFile(vectorStores, name, WORKFLOWS)) {
            String cleanedDocument = preprocessDocument(json);

            if (!cleanedDocument.isEmpty()) {
                // Split the document into chunks
                List<String> chunks = splitDocument(cleanedDocument.split("\\s+"));

                for (String chunk : chunks) {
                    documents.add(new Document(chunk, Map.of(CATEGORY, WORKFLOWS, NAME, name)));
                }
            }
        }
    }

    private static void addToDocuments(
        String name, String componentName, String json, List<Document> documents, String type) {

        String cleanedDocument = preprocessDocument(json);

        if (!cleanedDocument.isEmpty()) {
            // Split the document into chunks
            List<String> chunks = splitDocument(cleanedDocument.split("\\s+"));

            for (String chunk : chunks) {
                documents.add(new Document(chunk,
                    Map.of(CATEGORY, COMPONENTS, NAME, name, TYPE, type, COMPONENT_NAME, componentName)));
            }
        }
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

        List<PropertyDecorator> properties = PropertyDecorator.toPropertyDecorators(actionDefinition.getProperties());

        if (!properties.isEmpty()) {
            json.append("\"parameters\": ")
                .append(getObjectValue(properties));
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

        List<PropertyDecorator> properties = PropertyDecorator.toPropertyDecorators(triggerDefinition.getProperties());

        if (!properties.isEmpty()) {
            json.append("\"parameters\": ")
                .append(getObjectValue(properties));
        }
        return json.append("}")
            .toString();
    }

    private String getArrayValue(List<PropertyDecorator> properties) {
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

    private String getSampleValue(PropertyDecorator property) {
        return switch (property.getType()) {
            case ARRAY -> getArrayValue(property.getItems());
            case BOOLEAN -> "false";
            case DATE -> "\"1980-01-01\"";
            case DATE_TIME -> "\"1980-01-01T00:00:00\"";
            case DYNAMIC_PROPERTIES -> "{}";
            case INTEGER -> "1";
            case NUMBER -> "0.0";
            case OBJECT -> getObjectValue(property.getObjectProperties());
            case FILE_ENTRY -> getObjectValue(property.getFileEntryProperties());
            case TIME -> "\"00:00:00\"";
            default -> "\"\"";
        };
    }

    private String getObjectValue(List<PropertyDecorator> properties) {
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

    private void initializeVectorStoreTable(Path documentationPath) {
        if (vectorStoreService.count() > 0) {
            List<Map<String, Object>> vectorsMetadataList = vectorStoreService.findAll()
                .stream()
                .map(com.bytechef.ee.ai.copilot.domain.VectorStore::getMetadata)
                .toList();

            storeDocuments(vectorsMetadataList, documentationPath);
        } else {
            storeDocuments(List.of(), documentationPath);
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
    private static List<String> splitDocument(String[] tokens) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int tokenCount = 0;

        for (String token : tokens) {
            if (tokenCount + 1 > MAX_TOKENS) {
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

    private String toString(ComponentDefinition componentDefinition) {
        StringBuilder definitionText = new StringBuilder();

        definitionText.append("Component Name: ")
            .append(componentDefinition.getName())
            .append(",\n")
            .append("Description: ")
            .append(componentDefinition.getDescription())
            .append(",\n");

        return definitionText.toString();
    }

    private String toString(TriggerDefinition triggerDefinition) {
        StringBuilder definitionText = new StringBuilder();

        definitionText.append("Trigger Name: ")
            .append(triggerDefinition.getName())
            .append(",\n")
            .append("Description: ")
            .append(triggerDefinition.getDescription())
            .append(",\n")
            .append("Example JSON Structure: \n")
            .append(createJsonExample(triggerDefinition))
            .append(";\n");

        OutputResponse outputResponse = triggerDefinition.getOutputResponse();

        if (triggerDefinition.isOutputDefined() && outputResponse != null && outputResponse.outputSchema() != null) {
            definitionText.append("Output JSON: \n")
                .append(getSampleValue(new PropertyDecorator(outputResponse.outputSchema())))
                .append(";\n");
        }

        return definitionText.toString();
    }

    private String toString(ActionDefinition actionDefinition) {
        StringBuilder definitionText = new StringBuilder();

        String name = actionDefinition.getName();

        if (!name.equals("customAction")) {

            definitionText.append("Action Name: ")
                .append(name)
                .append(",\n")
                .append("Description: ")
                .append(actionDefinition.getDescription())
                .append(",\n")
                .append("Example JSON Structure: \n")
                .append(createJsonExample(actionDefinition))
                .append(";\n");

            OutputResponse outputResponse = actionDefinition.getOutputResponse();

            if (actionDefinition.isOutputDefined() && outputResponse != null) {
                definitionText.append("Output JSON: \n")
                    .append(getSampleValue(new PropertyDecorator(outputResponse.outputSchema())))
                    .append(";\n");
            }
        }

        return definitionText.toString();
    }

    private void storeDocuments(
        TokenCountBatchingStrategy batchingStrategy, List<Map<String, Object>> vectorStores, VectorStore vectorStore) {

        List<Document> documentList = new ArrayList<>();

        for (ComponentDefinition componentDefinition : componentDefinitionService.getComponentDefinitions()) {
            String json = toString(componentDefinition);
            String componentName = componentDefinition.getName();

            List<TriggerDefinition> triggers = componentDefinition.getTriggers();

            if (!triggers.isEmpty()) {
                for (TriggerDefinition triggerDefinition : triggers) {
                    if (!containsFile(vectorStores, triggerDefinition.getName(), componentName, COMPONENTS)) {
                        String triggerJson = json + toString(triggerDefinition);

                        addToDocuments(triggerDefinition.getName(), componentName, triggerJson, documentList,
                            "trigger");
                    }
                }
            }

            List<ActionDefinition> actions = componentDefinition.getActions();

            if (!actions.isEmpty()) {
                for (ActionDefinition actionDefinition : actions) {
                    if (!containsFile(vectorStores, actionDefinition.getName(), componentName, COMPONENTS)) {
                        String actionJson = json + toString(actionDefinition);

                        addToDocuments(actionDefinition.getName(), componentName, actionJson, documentList, "action");
                    }
                }
            }
        }

        for (List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private static boolean containsFile(
        List<Map<String, Object>> vectorStoreList, String fileName, String categoryName) {

        return !vectorStoreList.isEmpty() && vectorStoreList.stream()
            .anyMatch(map -> fileName.equals(map.get(NAME)) && categoryName.equals(map.get(CATEGORY)));
    }

    private static boolean containsFile(
        List<Map<String, Object>> vectorStoreList, String fileName, String componentName, String categoryName) {

        return !vectorStoreList.isEmpty() && vectorStoreList.stream()
            .anyMatch(map -> fileName.equals(map.get(NAME)) && componentName.equals(map.get(COMPONENT_NAME))
                && categoryName.equals(map.get(CATEGORY)));
    }

    private static void storeDocumentsFromPath(
        Path path, String suffix, BatchingStrategy batchingStrategy,
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

                    addToDocuments(vectorStoreList, fileName, document, documentList);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        for (List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private void storeDocuments(
        List<Map<String, Object>> vectorStoreList, Path workflowsPath) {

        try {
            storeDocumentsFromPath(workflowsPath, ".json", strategy, vectorStoreList, vectorStore);
            storeDocuments(strategy, vectorStoreList, vectorStore);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class PropertyDecorator {

        enum Location {
            COMPONENT,
            TASK_DISPATCHER
        }

        private final BaseProperty property;
        private final Type type;
        private final Location location;

        public PropertyDecorator(BaseProperty property) {
            this.property = property;

            switch (property) {
                case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty ignored -> {
                    this.type = Type.ARRAY;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.ArrayProperty ignored -> {
                    this.type = Type.ARRAY;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.BooleanProperty ignored -> {
                    this.type = Type.BOOLEAN;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.BooleanProperty ignored -> {
                    this.type = Type.BOOLEAN;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.DateProperty ignored -> {
                    this.type = Type.DATE;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.DateProperty ignored -> {
                    this.type = Type.DATE;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.DateTimeProperty ignored -> {
                    this.type = Type.DATE_TIME;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.DateTimeProperty ignored -> {
                    this.type = Type.DATE_TIME;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.component.domain.DynamicPropertiesProperty ignored -> {
                    this.type = Type.DYNAMIC_PROPERTIES;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.IntegerProperty ignored -> {
                    this.type = Type.INTEGER;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.IntegerProperty ignored -> {
                    this.type = Type.INTEGER;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty ignored -> {
                    this.type = Type.FILE_ENTRY;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.FileEntryProperty ignored -> {
                    this.type = Type.FILE_ENTRY;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.NullProperty ignored -> {
                    this.type = Type.NULL;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.NumberProperty ignored -> {
                    this.type = Type.NUMBER;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.NumberProperty ignored -> {
                    this.type = Type.NUMBER;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty ignored -> {
                    this.type = Type.OBJECT;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.ObjectProperty ignored -> {
                    this.type = Type.OBJECT;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.StringProperty ignored -> {
                    this.type = Type.STRING;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.StringProperty ignored -> {
                    this.type = Type.STRING;
                    this.location = Location.COMPONENT;
                }
                case com.bytechef.platform.workflow.task.dispatcher.domain.TimeProperty ignored -> {
                    this.type = Type.TIME;
                    this.location = Location.TASK_DISPATCHER;
                }
                case com.bytechef.platform.component.domain.TimeProperty ignored -> {
                    this.type = Type.TIME;
                    this.location = Location.COMPONENT;
                }
                default -> {
                    this.type = Type.NULL;
                    this.location = Location.COMPONENT;
                }
            }
        }

        public List<PropertyDecorator> getItems() {
            return switch (location) {
                case TASK_DISPATCHER -> toPropertyDecorators(
                    ((com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty) property).getItems());
                case COMPONENT ->
                    toPropertyDecorators(((com.bytechef.platform.component.domain.ArrayProperty) property).getItems());
            };
        }

        public List<PropertyDecorator> getFileEntryProperties() {
            return switch (location) {
                case TASK_DISPATCHER -> toPropertyDecorators(
                    ((com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty) property)
                        .getProperties());
                case COMPONENT -> toPropertyDecorators(
                    ((com.bytechef.platform.component.domain.FileEntryProperty) property).getProperties());
            };
        }

        public String getName() {
            return property.getName();
        }

        public List<PropertyDecorator> getObjectProperties() {
            return switch (location) {
                case TASK_DISPATCHER -> toPropertyDecorators(
                    ((com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty) property).getProperties());
                case COMPONENT -> toPropertyDecorators(
                    ((com.bytechef.platform.component.domain.ObjectProperty) property).getProperties());
            };
        }

        public Type getType() {
            return type;
        }

        public static List<PropertyDecorator> toPropertyDecorators(List<? extends BaseProperty> properties) {
            return properties.stream()
                .map(PropertyDecorator::new)
                .toList();
        }
    }
}
