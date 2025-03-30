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

    private final ApplicationProperties.Ai.Paths paths;
    private final TokenCountBatchingStrategy strategy;
    private final VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;

    @SuppressFBWarnings("EI")
    @Autowired
    public VectorStoreConfiguration(
        VectorStore vectorStore, VectorStoreService vectorStoreService, ApplicationProperties applicationProperties) {

        this.paths = applicationProperties.getAi()
            .getPaths();
        this.vectorStore = vectorStore;
        this.vectorStoreService = vectorStoreService;
        this.strategy = new TokenCountBatchingStrategy(
            EncodingType.CL100K_BASE, 8191, 0.1, Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);
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
            Paths.get(paths.getComponentsPath()), Paths.get(paths.getDocumentationPath()),
            Paths.get(paths.getWelcomePath()),
            Paths.get(paths.getWorkflowsPath()));
    }

    private void initializeVectorStoreTable(
        Path componentsPath, Path documentationPath, Path welcomePath, Path workflowsPath) {

        if (vectorStoreService.count() > 0) {
            List<com.bytechef.platform.ai.domain.VectorStore> vectorsStores = vectorStoreService.findAll();
            List<Map<String, Object>> vectorsMetadataList = vectorsStores.stream()
                .map(com.bytechef.platform.ai.domain.VectorStore::getMetadata)
                .toList();

            storeDocuments(vectorsMetadataList, componentsPath, documentationPath, welcomePath, workflowsPath);
        } else {
            storeDocuments(List.of(), componentsPath, documentationPath, welcomePath, workflowsPath);
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

                    // check if already exists
                    if (vectorStoreListContainsFile(vectorStoreList, fileName, categoryName)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String document = Files.readString(filePath);

                    // Preprocess the document
                    String cleanedDocument = preprocessDocument(document);

                    if (!cleanedDocument.isEmpty()) {
                        // Split the document into chunks
                        List<String> chunks = splitDocument(cleanedDocument.split("\\s+"), 1536);

                        for (String chunk : chunks) {
                            // TODO add versioning to files
                            documentList.add(new Document(chunk, Map.of(CATEGORY, categoryName, NAME, fileName)));
                        }
                    }

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
        List<Map<String, Object>> vectorStoreList, Path componentsPath, Path documentationPath, Path welcomePath,
        Path workflowsPath) {

        try {
            storeDocumentsFromPath(DOCUMENTATION, documentationPath, ".md", strategy, vectorStoreList, vectorStore);
            storeDocumentsFromPath(WORKFLOWS, workflowsPath, ".json", strategy, vectorStoreList, vectorStore);
            storeDocumentsFromPath(COMPONENTS, componentsPath, ".md", strategy, vectorStoreList, vectorStore);
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
