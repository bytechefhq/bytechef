/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.audit.ApiConnectorAuditEvents;
import com.bytechef.ee.platform.apiconnector.configuration.audit.ApiConnectorAuditMapper;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacade;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacadeImpl;
import com.bytechef.ee.platform.apiconnector.configuration.generator.OpenApiGenerator;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorAiService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.ee.platform.apiconnector.configuration.service.OpenApiSpecificationGenerator;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    ApiConnectorAuditIntTest.ApiConnectorFacadeConfiguration.class, PostgreSQLContainerConfiguration.class
})
class ApiConnectorAuditIntTest {

    private static final long API_CONNECTOR_ID = 23L;
    private static final String DOCUMENTATION_URL = "https://docs.example.com/api?token=confidential";
    private static final String JOB_ID = "generation-job";
    private static final String SPECIFICATION = """
        openapi: 3.0.1
        info:
          title: Audit Sample
          version: 1.0.0
        paths:
          /items:
            get:
              operationId: listItems
              responses:
                '200':
                  description: OK
        """;

    @Autowired
    private ApiConnectorFacade apiConnectorFacade;

    @MockitoBean
    private ApiConnectorAiService apiConnectorAiService;

    @MockitoBean
    private ApiConnectorFileStorage apiConnectorFileStorage;

    @MockitoBean
    private ApiConnectorGenerationJobService apiConnectorGenerationJobService;

    @MockitoBean
    private ApiConnectorService apiConnectorService;

    @MockitoBean
    private OpenApiSpecificationGenerator openApiSpecificationGenerator;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectorChangesRecordTheConnectorAndItsName() {
        ApiConnector apiConnector = createApiConnector("auditSample");

        when(apiConnectorService.create(any())).thenReturn(apiConnector);
        when(apiConnectorService.update(any())).thenReturn(apiConnector);

        apiConnectorFacade.createApiConnector(apiConnector);

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_CREATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID))
            .containsEntry("name", "auditSample");

        apiConnectorFacade.updateApiConnector(apiConnector);

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID))
            .containsEntry("name", "auditSample");

        apiConnectorFacade.enableApiConnector(API_CONNECTOR_ID, false);

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_ENABLED_UPDATED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID))
            .containsEntry("enabled", "false");

        apiConnectorFacade.deleteApiConnector(API_CONNECTOR_ID);

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_DELETED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID));
    }

    @Test
    void testImportAndGenerateRecordTheConnectorWithoutTheSpecification() throws Exception {
        when(apiConnectorFileStorage.storeApiConnectorDefinition(anyString(), anyString()))
            .thenReturn(new FileEntry("definition.json", "file:///definition.json"));
        when(apiConnectorFileStorage.storeApiConnectorSpecification(anyString(), anyString()))
            .thenReturn(new FileEntry("specification.yaml", "file:///specification.yaml"));
        when(apiConnectorService.fetchApiConnector(anyString(), anyInt())).thenReturn(Optional.empty());
        when(apiConnectorService.create(any())).thenAnswer(invocation -> {
            ApiConnector createdApiConnector = invocation.getArgument(0);

            createdApiConnector.setId(API_CONNECTOR_ID);

            return createdApiConnector;
        });
        when(apiConnectorAiService.generateOpenApiSpecification(DOCUMENTATION_URL)).thenReturn(SPECIFICATION);

        Path definitionPath = Files.createTempFile("definition", ".json");

        Files.writeString(definitionPath, "{}");

        try (MockedStatic<OpenApiGenerator> openApiGeneratorMockedStatic = mockStatic(OpenApiGenerator.class)) {
            openApiGeneratorMockedStatic.when(() -> OpenApiGenerator.generate(anyString(), any()))
                .thenReturn(definitionPath);

            ApiConnector importedApiConnector = apiConnectorFacade.importOpenApiSpecification(
                "auditSample", SPECIFICATION);

            assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_IMPORTED))
                .containsEntry("result", "SUCCESS")
                .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID))
                .containsEntry("name", importedApiConnector.getName())
                .doesNotContainValue(SPECIFICATION);

            persistenceAuditEventRepository.deleteAll();

            apiConnectorFacade.generateFromDocumentation("auditSample", DOCUMENTATION_URL);

            assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_GENERATED))
                .containsEntry("result", "SUCCESS")
                .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID))
                .containsEntry("name", importedApiConnector.getName())
                .doesNotContainValue(DOCUMENTATION_URL)
                .doesNotContainValue(SPECIFICATION);
            assertThat(findEventTypes()).containsExactly(ApiConnectorAuditEvents.API_CONNECTOR_GENERATED);
        } finally {
            Files.deleteIfExists(definitionPath);
        }
    }

    @Test
    void testGenerationJobStartAndCancelRecordTheJob() {
        when(apiConnectorGenerationJobService.create(DOCUMENTATION_URL))
            .thenReturn(new ApiConnectorGenerationJob(JOB_ID, DOCUMENTATION_URL));
        when(apiConnectorGenerationJobService.requestCancellation(JOB_ID)).thenReturn(true);

        apiConnectorFacade.startGenerationJob(DOCUMENTATION_URL, "confidential prompt");

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_GENERATION_JOB_STARTED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("jobId", JOB_ID)
            .doesNotContainValue(DOCUMENTATION_URL)
            .doesNotContainValue("confidential prompt");

        apiConnectorFacade.cancelGenerationJob(JOB_ID);

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_GENERATION_JOB_CANCELLED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("jobId", JOB_ID);
    }

    @Test
    void testCancelOfAnUnknownGenerationJobIsNotRecorded() {
        when(apiConnectorGenerationJobService.requestCancellation(JOB_ID)).thenReturn(false);

        assertThat(apiConnectorFacade.cancelGenerationJob(JOB_ID)).isFalse();

        assertThat(findEventTypes()).isEmpty();
    }

    @Test
    void testDeleteOfAnUnknownConnectorRecordsAnError() {
        doThrow(new NoSuchElementException())
            .when(apiConnectorService)
            .delete(API_CONNECTOR_ID);

        assertThatThrownBy(() -> apiConnectorFacade.deleteApiConnector(API_CONNECTOR_ID))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(ApiConnectorAuditEvents.API_CONNECTOR_DELETED))
            .containsEntry("result", "ERROR")
            .containsEntry("apiConnectorId", String.valueOf(API_CONNECTOR_ID))
            .containsEntry("errorClass", NoSuchElementException.class.getName());
    }

    private static ApiConnector createApiConnector(String name) {
        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setId(API_CONNECTOR_ID);
        apiConnector.setName(name);

        return apiConnector;
    }

    private List<String> findEventTypes() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        return persistentAuditEvents.stream()
            .map(PersistentAuditEvent::getEventType)
            .toList();
    }

    private Map<String, String> findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");

        return persistentAuditEvent.getData();
    }

    @Configuration
    @Import(ApiConnectorAuditMapper.class)
    static class ApiConnectorFacadeConfiguration {

        @Bean
        ApiConnectorFacade apiConnectorFacade(
            ApiConnectorAiService apiConnectorAiService, ApiConnectorFileStorage apiConnectorFileStorage,
            ApiConnectorGenerationJobService apiConnectorGenerationJobService,
            ApiConnectorService apiConnectorService, OpenApiSpecificationGenerator openApiSpecificationGenerator) {

            return new ApiConnectorFacadeImpl(
                apiConnectorAiService, apiConnectorFileStorage, apiConnectorGenerationJobService, apiConnectorService,
                openApiSpecificationGenerator);
        }
    }
}
