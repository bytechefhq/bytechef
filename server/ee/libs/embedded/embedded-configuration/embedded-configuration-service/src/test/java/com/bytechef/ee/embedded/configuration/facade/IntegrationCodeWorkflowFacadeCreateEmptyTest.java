/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoader;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies the integration starter templates and {@link IntegrationCodeWorkflowFacadeImpl#createEmptyCodeWorkflow}.
 *
 * <p>
 * The correctness gate is that each rendered starter template actually loads through the real polyglot
 * {@link IntegrationHandlerLoader} into an {@link IntegrationDefinition} with the substituted component name and at
 * least one workflow. Python and Ruby are guarded by an availability assumption so CI without graalpy / truffleruby
 * skips rather than fails. JavaScript is always available.
 *
 * <p>
 * The facade tests exercise the JavaScript deploy path (services mocked, real loader) and the validation branches.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class IntegrationCodeWorkflowFacadeCreateEmptyTest {

    @Test
    void testJavaScriptStarterLoadsThroughLoader() throws IOException {
        assertStarterLoads(Language.JAVASCRIPT);
    }

    @Test
    void testPythonStarterLoadsThroughLoader() throws IOException {
        assumePolyglotAvailable("python");

        assertStarterLoads(Language.PYTHON);
    }

    @Test
    void testRubyStarterLoadsThroughLoader() throws IOException {
        assumePolyglotAvailable("ruby");

        assertStarterLoads(Language.RUBY);
    }

    @Test
    void testCreateEmptyCodeWorkflowDeploysLoadableJavaScriptIntegration() {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationService integrationService = mock(IntegrationService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        Integration createdIntegration = new Integration();

        createdIntegration.setId(1L);
        createdIntegration.setComponentName("my-integration");
        createdIntegration.setName("my-integration");

        when(integrationService.fetchIntegration("my-integration")).thenReturn(Optional.empty());
        when(integrationService.create(any()))
            .thenReturn(createdIntegration);

        when(integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L)).thenReturn(Optional.empty());

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            codeWorkflowContainer, Map.of(), Map.of());

        when(codeWorkflowContainerFacade.create(any(), any(), any(), eq(Language.JAVASCRIPT), any(), any(), any()))
            .thenReturn(reconciliation);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(TagService.class), mock(WorkflowService.class), List.of());

        Integration integration = integrationCodeWorkflowFacade.createEmptyCodeWorkflow(
            "my-integration", Language.JAVASCRIPT);

        assertThat(integration.getId()).isEqualTo(1L);
        assertThat(integration.getComponentName()).isEqualTo("my-integration");

        ArgumentCaptor<Integration> integrationCaptor = ArgumentCaptor.forClass(Integration.class);

        verify(integrationService).create(integrationCaptor.capture());

        assertThat(integrationCaptor.getValue()
            .getComponentName()).isEqualTo("my-integration");

        verify(integrationCodeWorkflowService).create(eq(codeWorkflowContainer), any());
        verify(integrationService, never()).publishIntegration(anyLong(), any());
    }

    // RUBY-DISABLED: RUBY joins JAVA in the rejected set — org.graalvm.polyglot:ruby is published only up to 25.0.0
    // and crashes on the Truffle 25.2.4 this repo pins, so create-empty rejects it with LANGUAGE_NOT_SUPPORTED rather
    // than rendering a starter it cannot load. Drop RUBY from names below once a polyglot ruby jar built on Truffle
    // 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED.
    @ParameterizedTest
    @EnumSource(value = Language.class, names = {
        "JAVA", "RUBY"
    })
    void testCreateEmptyCodeWorkflowRejectsUnsupportedLanguage(Language language) {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationService integrationService = mock(IntegrationService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(TagService.class), mock(WorkflowService.class), List.of());

        assertThatThrownBy(
            () -> integrationCodeWorkflowFacade.createEmptyCodeWorkflow("my-integration", language))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED.getErrorKey());

        verify(integrationService, never()).create(any());
    }

    @Test
    void testCreateEmptyCodeWorkflowAllowsASecondIntegrationOnTheSameComponent() {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationService integrationService = mock(IntegrationService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        Integration existingIntegration = new Integration();

        existingIntegration.setId(2L);
        existingIntegration.setComponentName("my-integration");

        Integration createdIntegration = new Integration();

        createdIntegration.setId(3L);
        createdIntegration.setComponentName("my-integration");

        when(integrationService.fetchIntegration("my-integration")).thenReturn(Optional.of(existingIntegration));
        when(integrationService.create(any())).thenReturn(createdIntegration);
        when(
            codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    new CodeWorkflowContainerFacade.CodeWorkflowReconciliation(
                        mock(CodeWorkflowContainer.class), Map.of(), Map.of()));

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(TagService.class), mock(WorkflowService.class), List.of());

        // A component backs as many integrations as the user wants; they are told apart by name.
        Integration integration = integrationCodeWorkflowFacade.createEmptyCodeWorkflow(
            "my-integration", Language.JAVASCRIPT);

        assertThat(integration.getId()).isEqualTo(3L);

        verify(integrationService).create(any());
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsBlankName() {
        assertNameRejected("   ");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingQuote() {
        assertNameRejected("acme\", componentVersion: 9, x: (");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingBackslash() {
        assertNameRejected("acme\\bad");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingNewline() {
        assertNameRejected("acme\nbad");
    }

    private void assertNameRejected(String componentName) {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationService integrationService = mock(IntegrationService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, integrationService, integrationWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(TagService.class), mock(WorkflowService.class), List.of());

        assertThatThrownBy(
            () -> integrationCodeWorkflowFacade.createEmptyCodeWorkflow(componentName, Language.JAVASCRIPT))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME.getErrorKey());

        verify(integrationService, never()).create(any());
        verify(integrationService, never()).fetchIntegration(any());
    }

    private static void assertStarterLoads(Language language) throws IOException {
        String template = readTemplate(language).replace("__NAME__", "my-integration");

        Path path = Files.createTempFile("code_workflow_starter", "." + language.getExtension());

        Files.write(path, template.getBytes(StandardCharsets.UTF_8));

        URI uri = path.toUri();

        try {
            IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
                uri.toURL(), language, IntegrationHandlerLoader.JavaLoader.CLASS_LOADER,
                uri + UUID.randomUUID()
                    .toString(),
                mock(CacheManager.class));

            IntegrationDefinition integrationDefinition = integrationHandler.getDefinition();

            assertThat(integrationDefinition.getComponentName()).isEqualTo("my-integration");
            assertThat(integrationDefinition.getWorkflows()).isPresent();
            assertThat(integrationDefinition.getWorkflows()
                .get()).isNotEmpty();
            assertThat(integrationDefinition.getWorkflows()
                .get()
                .getFirst()
                .getName()).isEqualTo("my-workflow");
        } finally {
            Files.delete(path);
        }
    }

    private static void assumePolyglotAvailable(String languageId) {
        try (Context context = Context.create()) {
            context.eval(languageId, "1");
        } catch (RuntimeException e) {
            Assumptions.assumeTrue(false, "Polyglot language '" + languageId + "' is not available: " + e.getMessage());
        }
    }

    private static String readTemplate(Language language) throws IOException {
        String resource = "integration-code-workflow-templates/starter." + language.getExtension();

        try (InputStream inputStream = IntegrationCodeWorkflowFacadeCreateEmptyTest.class.getClassLoader()
            .getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Workflow workflow = new ApplicationProperties.Workflow();

        workflow.getCodeWorkflow()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWorkflow()).thenReturn(workflow);

        return applicationProperties;
    }
}
