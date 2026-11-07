/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_TAG_NAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.knowledgebase.remote.client.config.KnowledgeBaseRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.KnowledgeBaseProtoMapper;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.KnowledgeBaseRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader.Chunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.vectorstore.KnowledgeBaseVectorStoreWrapper;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = KnowledgeBaseRemoteClientIntTestConfiguration.class,
    properties = {
        "spring.ai.vectorstore.pgvector.dimensions=1536", "spring.ai.vectorstore.pgvector.index-type=hnsw",
        "spring.ai.vectorstore.pgvector.distance-type=cosine_distance"
    })
class RemoteKnowledgeBaseClientsIntTest {

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("knowledgeBasePgVectorStore")
    private VectorStore vectorStore;

    private long knowledgeBaseAId;
    private long knowledgeBaseBId;
    private ManagedChannel managedChannel;
    private RemoteKnowledgeBaseDocumentChunkFacadeClient remoteKnowledgeBaseDocumentChunkFacadeClient;
    private RemoteKnowledgeBaseDocumentLoaderClient remoteKnowledgeBaseDocumentLoaderClient;
    private RemoteKnowledgeBaseDocumentServiceClient remoteKnowledgeBaseDocumentServiceClient;
    private RemoteKnowledgeBaseDocumentTagServiceClient remoteKnowledgeBaseDocumentTagServiceClient;
    private RemoteKnowledgeBaseServiceClient remoteKnowledgeBaseServiceClient;
    private RemoteKnowledgeBaseVectorStore remoteKnowledgeBaseVectorStore;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        knowledgeBaseAId = createKnowledgeBase("Knowledge Base A");
        knowledgeBaseBId = createKnowledgeBase("Knowledge Base B");

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new KnowledgeBaseRemoteServiceGrpcServer(
                        knowledgeBaseService, knowledgeBaseDocumentService, knowledgeBaseDocumentTagService,
                        knowledgeBaseDocumentChunkFacade, knowledgeBaseDocumentLoader, vectorStore,
                        new KnowledgeBaseProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteKnowledgeBaseDocumentChunkFacadeClient = new RemoteKnowledgeBaseDocumentChunkFacadeClient(
            fixedGrpcChannels, objectMapper);
        remoteKnowledgeBaseDocumentLoaderClient = new RemoteKnowledgeBaseDocumentLoaderClient(
            fixedGrpcChannels, objectMapper);
        remoteKnowledgeBaseDocumentServiceClient = new RemoteKnowledgeBaseDocumentServiceClient(
            fixedGrpcChannels, objectMapper);
        remoteKnowledgeBaseDocumentTagServiceClient = new RemoteKnowledgeBaseDocumentTagServiceClient(
            fixedGrpcChannels, objectMapper);
        remoteKnowledgeBaseServiceClient = new RemoteKnowledgeBaseServiceClient(fixedGrpcChannels, objectMapper);
        remoteKnowledgeBaseVectorStore = new RemoteKnowledgeBaseVectorStore(fixedGrpcChannels, objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        for (KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk : knowledgeBaseDocumentChunkRepository.findAll()) {
            String vectorStoreId = knowledgeBaseDocumentChunk.getVectorStoreId();

            if (vectorStoreId != null) {
                vectorStore.delete(List.of(vectorStoreId));
            }
        }

        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testGetKnowledgeBasesAndTagsMatchTheHost() {
        long knowledgeBaseDocumentId = remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "guide.txt", List.of(new Chunk(null, "about invoices", Map.of())), Map.of());

        knowledgeBaseDocumentTagService.updateTagNames(knowledgeBaseDocumentId, List.of("finance"));

        assertThat(remoteKnowledgeBaseServiceClient.getKnowledgeBases())
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(knowledgeBaseService.getKnowledgeBases());
        assertThat(remoteKnowledgeBaseServiceClient.getKnowledgeBase(knowledgeBaseAId))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(knowledgeBaseService.getKnowledgeBase(knowledgeBaseAId));
        assertThat(remoteKnowledgeBaseDocumentServiceClient.getKnowledgeBaseDocuments(knowledgeBaseAId))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseAId));
        assertThat(remoteKnowledgeBaseDocumentChunkFacadeClient.getKnowledgeBaseDocumentChunksByDocumentId(
            knowledgeBaseDocumentId))
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(
                    knowledgeBaseDocumentId));
        assertThat(remoteKnowledgeBaseDocumentTagServiceClient.getAllTagNames()).containsExactly("finance");
        assertThat(remoteKnowledgeBaseDocumentTagServiceClient.getTagNamesByKnowledgeBaseId(knowledgeBaseAId))
            .containsExactly("finance");
        assertThat(remoteKnowledgeBaseDocumentTagServiceClient.getTagNamesByKnowledgeBaseId(knowledgeBaseBId))
            .isEmpty();
    }

    @Test
    void testLoadDocumentThroughTheClientStoresItOnTheHost() {
        long knowledgeBaseDocumentId = remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "guide.txt",
            List.of(
                new Chunk(null, "first chunk about invoices", Map.of("page", 1)),
                new Chunk(null, "second chunk about refunds", Map.of("page", 2))),
            Map.of("source", "workflow"));

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
            knowledgeBaseDocumentId);

        assertThat(knowledgeBaseDocument.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_READY);
        assertThat(knowledgeBaseDocument.getName()).isEqualTo("guide.txt");
        assertThat(knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(
            knowledgeBaseDocumentId)).hasSize(2);

        Document document = search(knowledgeBaseAId, null, "first chunk about invoices").getFirst();

        assertThat(document.getText()).isEqualTo("first chunk about invoices");
        assertThat(document.getMetadata()).containsEntry("source", "workflow");
        assertThat(((Number) document.getMetadata()
            .get(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID)).longValue()).isEqualTo(knowledgeBaseDocumentId);
    }

    @Test
    void testLoadDocumentFailureIsReportedAndMarksTheDocumentError() {
        assertThatThrownBy(() -> remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "broken.txt",
            List.of(new Chunk(null, KnowledgeBaseRemoteClientIntTestConfiguration.FAILING_TEXT, Map.of())),
            Map.of())).isInstanceOf(StatusRuntimeException.class);

        assertThat(knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseAId)).singleElement()
            .satisfies(knowledgeBaseDocument -> assertThat(knowledgeBaseDocument.getStatus())
                .isEqualTo(KnowledgeBaseDocument.STATUS_ERROR));
    }

    @Test
    void testSimilaritySearchWithAKnowledgeBaseAndTagFilterReturnsOnlyMatchingChunks() {
        String taggedKey = METADATA_TAG_NAMES + "_t1";

        remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "tagged.txt", List.of(new Chunk(null, "tagged in a", Map.of())),
            Map.of(taggedKey, true));
        remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "untagged.txt", List.of(new Chunk(null, "untagged in a", Map.of())), Map.of());
        remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseBId, null, "other.txt", List.of(new Chunk(null, "tagged in b", Map.of())),
            Map.of(taggedKey, true));

        List<Document> documents = search(knowledgeBaseAId, List.of("t1"), "tagged");

        assertThat(documents).extracting(Document::getText)
            .containsExactly("tagged in a");
    }

    @Test
    void testDeleteDocumentsByFilterRemovesThemOnTheHost() {
        long deletedKnowledgeBaseDocumentId = remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "deleted.txt", List.of(new Chunk(null, "to be deleted", Map.of())), Map.of());

        remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "kept.txt", List.of(new Chunk(null, "to be kept", Map.of())), Map.of());

        VectorStore knowledgeBaseVectorStore = new KnowledgeBaseVectorStoreWrapper(
            remoteKnowledgeBaseVectorStore, knowledgeBaseAId);
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        knowledgeBaseVectorStore.delete(
            filterExpressionBuilder.eq(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, deletedKnowledgeBaseDocumentId)
                .build());

        assertThat(search(knowledgeBaseAId, null, "to be")).extracting(Document::getText)
            .containsExactly("to be kept");
    }

    @Test
    void testUpdateDocumentReplacesTheChunk() {
        long knowledgeBaseDocumentId = remoteKnowledgeBaseDocumentLoaderClient.load(
            knowledgeBaseAId, null, "guide.txt", List.of(new Chunk(null, "old text", Map.of())), Map.of());

        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = remoteKnowledgeBaseDocumentChunkFacadeClient
            .getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId)
            .getFirst();

        remoteKnowledgeBaseDocumentLoaderClient.update(
            knowledgeBaseAId, knowledgeBaseDocumentId, knowledgeBaseDocumentChunk.getId(),
            List.of(new Chunk(null, "new text", Map.of())), Map.of());

        assertThat(search(knowledgeBaseAId, null, "text")).extracting(Document::getText)
            .containsExactly("new text");
    }

    @Test
    void testGetKnowledgeBaseOfAnUnknownIdIsReportedAsAnError() {
        assertThatThrownBy(() -> remoteKnowledgeBaseServiceClient.getKnowledgeBase(Long.MAX_VALUE))
            .isInstanceOf(StatusRuntimeException.class)
            .hasMessageContaining(String.valueOf(Long.MAX_VALUE));
    }

    private long createKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(name);

        return knowledgeBaseRepository.save(knowledgeBase)
            .getId();
    }

    private List<Document> search(long knowledgeBaseId, List<String> tagNames, String query) {
        VectorStore knowledgeBaseVectorStore = new KnowledgeBaseVectorStoreWrapper(
            remoteKnowledgeBaseVectorStore, knowledgeBaseId, tagNames);

        return knowledgeBaseVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
                .build());
    }
}
