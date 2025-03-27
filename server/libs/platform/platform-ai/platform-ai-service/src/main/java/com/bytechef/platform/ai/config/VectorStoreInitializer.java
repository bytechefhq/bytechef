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

import com.bytechef.platform.ai.service.VectorStoreService;
import com.knuddels.jtokkit.api.EncodingType;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class VectorStoreInitializer {

    private final org.springframework.ai.vectorstore.VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;
    private final TokenCountBatchingStrategy strategy;

    private static final String CATEGORY = "category";
    private static final String NAME = "name";

    private static final String DOCUMENTATION = "documentation";
    private static final String WORKFLOWS = "workflows";
    private static final String COMPONENTS = "components";
    private static final Path WELCOME_PATH =
        Paths.get("/home/user/IdeaProjects/bytechef/docs/src/content/docs/welcome.md");
    private static final Path DOCUMENTATION_PATH =
        Paths.get("/home/user/IdeaProjects/bytechef/docs/src/content/docs/automation");
    private static final Path COMPONENTS_PATH =
        Paths.get("/home/user/IdeaProjects/bytechef/docs/src/content/docs/reference/components");
    private static final Path WORKFLOWS_PATH = Paths.get(
        "/home/user/IdeaProjects/bytechef/server/libs/platform/platform-ai/platform-ai-service/src/main/resources/workflows");

    @Autowired
    public VectorStoreInitializer(org.springframework.ai.vectorstore.VectorStore vectorStore,
        VectorStoreService vectorStoreService) {
        this.vectorStore = vectorStore;
        this.vectorStoreService = vectorStoreService;
        this.strategy = new TokenCountBatchingStrategy(EncodingType.CL100K_BASE, 8191, 0.1,
            Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeOnStartup() {
        initializeVecorStoreTable(vectorStoreService);
    }

    private void initializeVecorStoreTable(VectorStoreService vectorStoreService) {
        if (vectorStoreService.count() > 0) {
            List<com.bytechef.platform.ai.domain.VectorStore> vectorsList = vectorStoreService.findAll();
            List<Map<String, Object>> vectorsMetadataList = vectorsList.stream()
                .map(com.bytechef.platform.ai.domain.VectorStore::getMetadata)
                .toList();
            addDocumentsToVectorDatabase(vectorsMetadataList);
        } else {
            addDocumentsToVectorDatabase(List.of());
        }
    }

    public static String preprocessDocument(String document) {
        Pattern htmlTagsPattern = Pattern.compile("<[^>]*>");
        document = htmlTagsPattern.matcher(document)
            .replaceAll("");

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
        //

        Pattern spacePattern = Pattern.compile("\\s+");
        document = spacePattern.matcher(document)
            .replaceAll(" ");

        return document.trim();
    }

    // Function to split a document into chunks based on a maximum token limit
    public static List<String> splitDocument(String[] tokens, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int tokenCount = 0;

        for (String token : tokens) {
            if (tokenCount + 1 > maxTokens) {
                chunks.add(currentChunk.toString()
                    .trim());
                currentChunk.setLength(0); // Reset the current chunk
                tokenCount = 0;
            }
            currentChunk.append(token)
                .append(" ");
            tokenCount++;
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString()
                .trim());
        }

        return chunks;
    }

    private static void storeDocumentsFromPath(
        String categoryName, Path path, String suffix, BatchingStrategy batchingStrategy,
        List<Map<String, Object>> vectorStoreList, org.springframework.ai.vectorstore.VectorStore vectorStore)
        throws IOException {
        List<Document> documentList = new ArrayList<>();

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString()
                    .endsWith(suffix)) {
                    String document = Files.readString(file);
                    String fileName = file.getFileName()
                        .toString()
                        .replace(suffix, "");

                    // check if already exists
                    if (vectorStoreListContainsFile(vectorStoreList, fileName, categoryName)) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Preprocess the document
                    String cleanedDocument = preprocessDocument(document);

                    if (!cleanedDocument.isEmpty()) {
                        // Split the document into chunks
                        List<String> chunks = splitDocument(cleanedDocument.split("\\s+"), 1536);

                        for (String chunk : chunks) {
                            // TODO: add versioning to files
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

    public static boolean
        vectorStoreListContainsFile(List<Map<String, Object>> vectorStoreList, String fileName, String categoryName) {
        return !vectorStoreList.isEmpty() && vectorStoreList.stream()
            .anyMatch(map -> fileName.equals(map.get(NAME)) && categoryName.equals(map.get(CATEGORY)));
    }

    private void addDocumentsToVectorDatabase(List<Map<String, Object>> vectorStoreList) {
        try {
            storeDocumentsFromPath(DOCUMENTATION, DOCUMENTATION_PATH, ".md", strategy, vectorStoreList, vectorStore);
            storeDocumentsFromPath(WORKFLOWS, WORKFLOWS_PATH, ".json", strategy, vectorStoreList, vectorStore);
            storeDocumentsFromPath(COMPONENTS, COMPONENTS_PATH, ".md", strategy, vectorStoreList, vectorStore);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            String welcome = Files.readString(WELCOME_PATH);
            String cleanedDocument = preprocessDocument(welcome);
            vectorStore.add(List.of(new Document(cleanedDocument, Map.of(CATEGORY, DOCUMENTATION, NAME, "welcome"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
