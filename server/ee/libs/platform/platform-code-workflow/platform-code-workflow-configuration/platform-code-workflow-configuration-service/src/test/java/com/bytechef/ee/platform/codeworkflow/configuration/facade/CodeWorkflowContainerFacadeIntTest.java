/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerServiceImpl;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorageImpl;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowDsl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = CodeWorkflowContainerFacadeIntTest.Config.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = WorkflowService.class)
class CodeWorkflowContainerFacadeIntTest {

    private static final String CODE_WORKFLOWS_DIR = "code_workflows";

    @Autowired
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Autowired
    private CodeWorkflowContainerRepository codeWorkflowContainerRepository;

    @Autowired
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Autowired
    private CodeWorkflowFileStorage codeWorkflowFileStorage;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void testDeleteDeletesTheContainerAndItsWorkflowsFile() {
        FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
            "container.js", "export default {}".getBytes(StandardCharsets.UTF_8));

        CodeWorkflowContainer codeWorkflowContainer = createCodeWorkflowContainer(workflowsFileEntry);

        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, workflowsFileEntry)).isTrue();

        codeWorkflowContainerFacade.delete(codeWorkflowContainer.getId());

        assertThat(codeWorkflowContainerRepository.findById(codeWorkflowContainer.getId())).isEmpty();
        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, workflowsFileEntry)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateStoresAnApiEndpointWorkflowWithAComponentTask() {
        when(workflowService.create(anyString(), eq(Workflow.Format.JSON), eq(Workflow.SourceType.JDBC)))
            .thenAnswer(invocation -> new Workflow(
                String.valueOf(UUID.randomUUID()), invocation.getArgument(0), Workflow.Format.JSON));

        WorkflowDefinition workflowDefinition = WorkflowDsl.workflow("orders-endpoint")
            .triggers(
                WorkflowDsl.trigger("request", "apiPlatform/v1/newApiRequest")
                    .parameters(Map.of("request", Map.of("method", "GET", "path", "orders"))))
            .tasks(
                WorkflowDsl.task("fetch_orders")
                    .perform((input, context) -> List.of("order-1")),
                WorkflowDsl.componentTask("respond", "apiPlatform/v1/responseToAPIRequest")
                    .parameters(Map.of("responseType", "SUCCESS", "response", Map.of("success", "${fetch_orders}"))));

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            "orders", "1", List.of(workflowDefinition), Language.JAVASCRIPT,
            "export default {}".getBytes(StandardCharsets.UTF_8), PlatformType.AUTOMATION);

        assertThat(codeWorkflowContainerRepository.findById(codeWorkflowContainer.getId())).isPresent();

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).create(definitionCaptor.capture(), eq(Workflow.Format.JSON),
            eq(Workflow.SourceType.JDBC));

        Workflow storedWorkflow = new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);

        List<WorkflowTask> workflowTasks = storedWorkflow.getTasks();

        WorkflowTask respondTask = workflowTasks.get(1);

        assertThat(respondTask.getType()).isEqualTo("apiPlatform/v1/responseToAPIRequest");

        Map<String, ?> respondParameters = respondTask.getParameters();

        Map<String, Object> response = (Map<String, Object>) respondParameters.get("response");

        assertThat(respondParameters.get("responseType")).isEqualTo("SUCCESS");
        assertThat(response).containsEntry("success", "${fetch_orders}");
    }

    private CodeWorkflowContainer createCodeWorkflowContainer(FileEntry workflowsFileEntry) {
        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        codeWorkflowContainer.setExternalVersion("1");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setWorkflows(workflowsFileEntry);

        return codeWorkflowContainerService.create(codeWorkflowContainer);
    }

    @EnableAutoConfiguration
    @Import({
        CodeWorkflowContainerFacadeIntTest.Config.CodeWorkflowContainerFacadeIntTestJdbcConfiguration.class,
        LiquibaseConfiguration.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        CodeWorkflowContainerFacade codeWorkflowContainerFacade(
            CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
            ObjectProvider<ComponentDefinitionService> componentDefinitionServiceProvider, ObjectMapper objectMapper,
            WorkflowService workflowService) {

            return new CodeWorkflowContainerFacadeImpl(
                codeWorkflowContainerService, codeWorkflowFileStorage, componentDefinitionServiceProvider, objectMapper,
                workflowService);
        }

        @Bean
        CodeWorkflowContainerService codeWorkflowContainerService(
            CodeWorkflowContainerRepository codeWorkflowContainerRepository) {

            return new CodeWorkflowContainerServiceImpl(codeWorkflowContainerRepository);
        }

        @Bean
        CodeWorkflowFileStorage codeWorkflowFileStorage(FileStorageService fileStorageService) {
            return new CodeWorkflowFileStorageImpl(fileStorageService);
        }

        @Bean
        FileStorageService fileStorageService() throws IOException {
            return new FilesystemFileStorageService(
                String.valueOf(Files.createTempDirectory("code-workflow-container-facade-int-test")));
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class CodeWorkflowContainerFacadeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            CodeWorkflowContainerFacadeIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new FileEntryToStringConverter(objectMapper), new StringToFileEntryConverter(objectMapper));
            }
        }
    }
}
