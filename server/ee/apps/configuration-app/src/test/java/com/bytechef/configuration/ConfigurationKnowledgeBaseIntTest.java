/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import io.grpc.BindableService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(
    classes = ConfigurationApplication.class, properties = {
        "bytechef.ai.knowledge-base.enabled=true", "bytechef.mail.host=mailpit", "bytechef.mail.port=1025",
        "spring.liquibase.contexts=configuration,execution"
    })
class ConfigurationKnowledgeBaseIntTest {

    private static final int DIMENSIONS = 1536;

    private static final PostgreSQLContainer<?> PG_VECTOR_CONTAINER = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private ApplicationContext applicationContext;

    @TestBean(name = "catalogEmbeddingModel")
    private EmbeddingModel embeddingModel;

    @Autowired
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        PG_VECTOR_CONTAINER.start();

        registry.add("bytechef.ai.vectorstore.pgvector.url", PG_VECTOR_CONTAINER::getJdbcUrl);
        registry.add("bytechef.ai.vectorstore.pgvector.username", PG_VECTOR_CONTAINER::getUsername);
        registry.add("bytechef.ai.vectorstore.pgvector.password", PG_VECTOR_CONTAINER::getPassword);
    }

    static EmbeddingModel catalogEmbeddingModel() {
        return new ConstantEmbeddingModel();
    }

    @Test
    void testKnowledgeBaseGrpcServiceIsServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains("bytechef.platform.knowledgebase.remote.v1.KnowledgeBaseRemoteService");
    }

    @Test
    void testKnowledgeBasesQueryIsServed() {
        assertThat(applicationContext.containsBean("knowledgeBaseGraphQlController")).isTrue();
    }

    @Test
    void testUploadedDocumentIsProcessedByTheInAppConsumer() throws InterruptedException {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Uploads");

        long knowledgeBaseId = knowledgeBaseService.createKnowledgeBase(knowledgeBase)
            .getId();

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentFacade.createKnowledgeBaseDocument(
            knowledgeBaseId, "notes.txt", "text/plain",
            new ByteArrayInputStream(
                "Invoices are due within thirty days of issue.".getBytes(StandardCharsets.UTF_8)));

        assertThat(awaitStatus(knowledgeBaseDocument.getId(), Duration.ofSeconds(30)))
            .isEqualTo(KnowledgeBaseDocument.STATUS_READY);
    }

    private int awaitStatus(long knowledgeBaseDocumentId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now()
            .plus(timeout);

        int status = knowledgeBaseDocumentService.getKnowledgeBaseDocument(knowledgeBaseDocumentId)
            .getStatus();

        while (status != KnowledgeBaseDocument.STATUS_READY && status != KnowledgeBaseDocument.STATUS_ERROR
            && Instant.now()
                .isBefore(deadline)) {

            Thread.sleep(100);

            status = knowledgeBaseDocumentService.getKnowledgeBaseDocument(knowledgeBaseDocumentId)
                .getStatus();
        }

        return status;
    }

    private static final class ConstantEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> instructions = request.getInstructions();

            for (int index = 0; index < instructions.size(); index++) {
                embeddings.add(new Embedding(embed(instructions.get(index)), index));
            }

            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public float[] embed(String text) {
            float[] vector = new float[DIMENSIONS];

            Arrays.fill(vector, 0.5f);

            return vector;
        }

        @Override
        public int dimensions() {
            return DIMENSIONS;
        }
    }
}
