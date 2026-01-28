/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.ee.ai.copilot.domain.CopilotVectorStore;
import com.bytechef.ee.ai.copilot.service.CopilotVectorStoreService;
import com.knuddels.jtokkit.api.EncodingType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
class CopilotVectorStoreLoaderConfiguration {

    private static final String CATEGORY = "category";
    private static final String CLASSPATH_DOCS_PATTERN = "classpath*:docs/**/*.md*";
    private static final String DOCS = "docs";
    private static final int MAX_TOKENS = 1536;
    private static final String NAME = "name";

    private final TokenCountBatchingStrategy batchingStrategy;
    private final CopilotVectorStoreService copilotVectorStoreService;
    private final ResourcePatternResolver resourcePatternResolver;
    private final VectorStore vectorStore;

    @SuppressFBWarnings(value = "EI")
    public CopilotVectorStoreLoaderConfiguration(
        CopilotVectorStoreService copilotVectorStoreService,
        @Qualifier("aiCopilotPgVectorStore") VectorStore vectorStore) {

        this.batchingStrategy = new TokenCountBatchingStrategy(
            EncodingType.CL100K_BASE, 8191, 0.1, Document.DEFAULT_CONTENT_FORMATTER, MetadataMode.ALL);
        this.copilotVectorStoreService = copilotVectorStoreService;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStartedEvent() {
        List<Map<String, Object>> vectorsMetadataList = getVectorsMetadataList();

        storeDocsDocuments(vectorsMetadataList);
    }

    private void addDocumentChunks(
        String name, List<Document> documents, String cleanedDocument, int hash) {

        List<String> chunks = splitDocument(cleanedDocument.split("\\s+"));

        for (String chunk : chunks) {
            documents.add(new Document(chunk, Map.of(CATEGORY, DOCS, NAME, name, "hash", hash)));
        }
    }

    private void addToDocuments(
        List<Map<String, Object>> vectorStoreMetadataList, String name, String content,
        List<Document> documents) {

        String cleanedDocument = preprocessDocument(content);
        int hash = cleanedDocument.hashCode();

        if (!containsVectorStoreFile(vectorStoreMetadataList, name)) {
            if (!cleanedDocument.isEmpty()) {
                addDocumentChunks(name, documents, cleanedDocument, hash);
            }
        } else {
            Optional<Map<String, Object>> vectorStoreFileMetadata = getVectorStoreFile(
                vectorStoreMetadataList, name);

            if (vectorStoreFileMetadata.isPresent()) {
                Map<String, Object> fileMetadata = vectorStoreFileMetadata.get();
                int vectorHash = (int) fileMetadata.get("hash");

                if (vectorHash != hash) {
                    deleteFromVectorStore(name, vectorHash);

                    addDocumentChunks(name, documents, cleanedDocument, hash);
                }
            }
        }
    }

    private static boolean containsVectorStoreFile(
        List<Map<String, Object>> vectorStoreList, String fileName) {

        return !vectorStoreList.isEmpty() && vectorStoreList.stream()
            .anyMatch(map -> fileName.equals(map.get(NAME)) && DOCS.equals(map.get(CATEGORY)));
    }

    private void deleteFromVectorStore(String name, int hash) {
        vectorStore.delete(String.format("name == '%s' AND category == '%s' AND hash == '%d'", name, DOCS, hash));
    }

    private static String extractDocName(Resource resource) {
        try {
            String uri = resource.getURI()
                .toString();
            int docsIndex = uri.indexOf("/docs/");

            if (docsIndex != -1) {
                return uri.substring(docsIndex + 6)
                    .replaceAll("\\.(md|mdx)$", "");
            }

            String filename = resource.getFilename();

            if (filename != null) {
                return filename.replaceAll("\\.(md|mdx)$", "");
            }
        } catch (IOException ignored) {
        }

        return "unknown";
    }

    private static Optional<Map<String, Object>> getVectorStoreFile(
        List<Map<String, Object>> vectorStoreList, String fileName) {

        return vectorStoreList.stream()
            .filter(map -> fileName.equals(map.get(NAME)) && DOCS.equals(map.get(CATEGORY)))
            .findFirst();
    }

    private List<Map<String, Object>> getVectorsMetadataList() {
        if (copilotVectorStoreService.count() > 0) {
            return copilotVectorStoreService.findAll()
                .stream()
                .map(CopilotVectorStore::getMetadata)
                .toList();
        }

        return List.of();
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
                currentChunk.setLength(0);

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

    private void storeDocsDocuments(List<Map<String, Object>> vectorsMetadataList) {
        List<Document> documentList = new ArrayList<>();

        storeDocsFromClasspath(vectorsMetadataList, documentList);

        for (List<Document> batch : batchingStrategy.batch(documentList)) {
            vectorStore.add(batch);
        }
    }

    private void storeDocsFromClasspath(List<Map<String, Object>> vectorsMetadataList, List<Document> documentList) {
        try {
            Resource[] resources = resourcePatternResolver.getResources(CLASSPATH_DOCS_PATTERN);

            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        String docName = extractDocName(resource);

                        addToDocuments(vectorsMetadataList, docName, content, documentList);
                    }
                }
            }
        } catch (IOException ioException) {
            throw new RuntimeException("Error loading docs from classpath", ioException);
        }
    }

}
