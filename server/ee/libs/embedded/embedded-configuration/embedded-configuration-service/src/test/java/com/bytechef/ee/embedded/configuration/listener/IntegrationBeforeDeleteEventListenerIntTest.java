/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationCodeWorkflowRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacadeImpl;
import com.bytechef.ee.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerServiceImpl;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorageImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
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
@SpringBootTest(classes = IntegrationBeforeDeleteEventListenerIntTest.Config.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = WorkflowService.class)
class IntegrationBeforeDeleteEventListenerIntTest {

    private static final String CODE_WORKFLOWS_DIR = "code_workflows";

    @Autowired
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Autowired
    private CodeWorkflowContainerRepository codeWorkflowContainerRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private IntegrationCodeWorkflowRepository integrationCodeWorkflowRepository;

    @Autowired
    private IntegrationCodeWorkflowService integrationCodeWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Test
    void testDeletingAnIntegrationDeletesItsCodeWorkflowsContainersAndFiles() {
        Integration integration = new Integration();

        integration.setComponentName("googleMail");
        integration.setName("Gmail");

        integration = integrationService.create(integration);

        CodeWorkflowContainer firstCodeWorkflowContainer = createCodeWorkflowContainer(integration, "first");

        integrationService.publishIntegration(integration.getId(), "first");

        integration = integrationService.getIntegration(integration.getId());

        CodeWorkflowContainer secondCodeWorkflowContainer = createCodeWorkflowContainer(integration, "second");

        FileEntry firstWorkflowsFileEntry = firstCodeWorkflowContainer.getWorkflows();
        FileEntry secondWorkflowsFileEntry = secondCodeWorkflowContainer.getWorkflows();

        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, firstWorkflowsFileEntry)).isTrue();
        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, secondWorkflowsFileEntry)).isTrue();

        integrationService.delete(integration.getId());

        assertThat(integrationService.fetchIntegration(integration.getComponentName())).isEmpty();
        assertThat(integrationCodeWorkflowRepository.findAll()).isEmpty();
        assertThat(codeWorkflowContainerRepository.findById(firstCodeWorkflowContainer.getId())).isEmpty();
        assertThat(codeWorkflowContainerRepository.findById(secondCodeWorkflowContainer.getId())).isEmpty();
        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, firstWorkflowsFileEntry)).isFalse();
        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, secondWorkflowsFileEntry)).isFalse();
    }

    private CodeWorkflowContainer createCodeWorkflowContainer(Integration integration, String name) {
        CodeWorkflowContainerFacade.CodeWorkflowReconciliation codeWorkflowReconciliation =
            codeWorkflowContainerFacade.create(
                name, "1", List.of(), Language.JAVASCRIPT, "export default {}".getBytes(StandardCharsets.UTF_8),
                PlatformType.EMBEDDED, Map.of());

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowReconciliation.codeWorkflowContainer();

        integrationCodeWorkflowService.create(codeWorkflowContainer, integration);

        return codeWorkflowContainer;
    }

    @EnableAutoConfiguration
    @Import({
        IntegrationBeforeDeleteEventListener.class,
        IntegrationBeforeDeleteEventListenerIntTest.Config.IntegrationBeforeDeleteEventListenerIntTestJdbcConfiguration.class,
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
        Encryption encryption(EncryptionKey encryptionKey) {
            return new EncryptionImpl(encryptionKey);
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @Bean
        FileStorageService fileStorageService() throws IOException {
            return new FilesystemFileStorageService(
                String.valueOf(Files.createTempDirectory("integration-before-delete-event-listener-int-test")));
        }

        @Bean
        IntegrationCodeWorkflowService integrationCodeWorkflowService(
            IntegrationCodeWorkflowRepository integrationCodeWorkflowRepository) {

            return new IntegrationCodeWorkflowServiceImpl(integrationCodeWorkflowRepository);
        }

        @Bean
        IntegrationService integrationService(IntegrationRepository integrationRepository) {
            return new IntegrationServiceImpl(integrationRepository);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class IntegrationBeforeDeleteEventListenerIntTestJdbcConfiguration
            extends AbstractIntTestJdbcConfiguration {

            private final Encryption encryption;
            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            IntegrationBeforeDeleteEventListenerIntTestJdbcConfiguration(
                Encryption encryption, ObjectMapper objectMapper) {

                this.encryption = encryption;
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                    new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                    new FileEntryToStringConverter(objectMapper), new MapWrapperToStringConverter(objectMapper),
                    new StringToFileEntryConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
            }
        }
    }
}
