/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.ee.ai.copilot.service.VectorStoreService;
import com.knuddels.jtokkit.api.EncodingType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class VectorStoreLoaderConfiguration {

    private static final String CATEGORY = "category";
    private static final int MAX_TOKENS = 1536;
    private static final String NAME = "name";
    private static final String README = "readme";
    private static final String ROOT = "user.dir";
    private static final String COMPONENTS_PATH = "server/libs/modules/components";
    private static final String README_PATH = "src/main/resources/README.mdx";

    private final TokenCountBatchingStrategy strategy;
    private final VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;

    @SuppressFBWarnings("EI")
    public VectorStoreLoaderConfiguration(
        VectorStore vectorStore, VectorStoreService vectorStoreService) {

        this.vectorStore = vectorStore;
        this.vectorStoreService = vectorStoreService;
        this.strategy = new TokenCountBatchingStrategy(
            EncodingType.CL100K_BASE, 8191, 0.1, Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStartedEvent() {
        Path projectRoot = Paths.get(System.getProperty(ROOT));
        Path componentsPath = projectRoot.resolve(COMPONENTS_PATH);

        if (Files.exists(componentsPath) && Files.isDirectory(componentsPath)) {
            initializeVectorStoreTable(componentsPath);
        }
    }

    private static void addToDocuments(
        List<Map<String, Object>> vectorStores, String name, String json, List<Document> documents) {

        if (!containsFile(vectorStores, name, README)) {
            String cleanedDocument = preprocessDocument(json);

            if (!cleanedDocument.isEmpty()) {
                // Split the document into chunks
                List<String> chunks = splitDocument(cleanedDocument.split("\\s+"));

                for (String chunk : chunks) {
                    documents.add(new Document(chunk, Map.of(CATEGORY, README, NAME, name)));
                }
            }
        }
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

    private static boolean containsFile(
        List<Map<String, Object>> vectorStoreList, String fileName, String categoryName) {

        return !vectorStoreList.isEmpty() && vectorStoreList.stream()
            .anyMatch(map -> fileName.equals(map.get(NAME)) && categoryName.equals(map.get(CATEGORY)));
    }

    private static void storeComponentDocuments(
        Path componentsBasePath, BatchingStrategy batchingStrategy,
        List<Map<String, Object>> vectorStoreList, VectorStore vectorStore) throws IOException {

        List<Document> documentList = new ArrayList<>();

        if (!Files.exists(componentsBasePath) || !Files.isDirectory(componentsBasePath)) {
            return;
        }

        try (var componentDirs = Files.list(componentsBasePath)) {
            componentDirs
                .filter(Files::isDirectory)
                .forEach(componentDir -> {
                    try {
                        Path readmePath = componentDir.resolve(README_PATH);

                        if (Files.exists(readmePath) && Files.isReadable(readmePath)) {
                            String componentName = componentDir.getFileName()
                                .toString();
                            String document = Files.readString(readmePath);

                            addToDocuments(vectorStoreList, componentName, document, documentList);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading README.mdx for component: " +
                            componentDir.getFileName(), e);
                    }
                });
        }

        for (List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private void storeDocuments(
        List<Map<String, Object>> vectorStoreList, Path componentsBasePath) {
        try {
            storeComponentDocuments(componentsBasePath, strategy, vectorStoreList, vectorStore);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
