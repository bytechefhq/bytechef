/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
