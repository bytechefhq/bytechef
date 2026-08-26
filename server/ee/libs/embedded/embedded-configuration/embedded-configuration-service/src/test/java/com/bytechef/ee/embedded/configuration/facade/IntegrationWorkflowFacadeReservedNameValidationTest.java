/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Proves that {@link IntegrationWorkflowFacadeImpl#addWorkflow} and
 * {@link IntegrationWorkflowFacadeImpl#updateWorkflow} reject a workflow input or top-level node named {@code vars} --
 * the reserved name the platform seeds resolved custom variables under at job-creation time (see
 * {@code JobInputConstants#VARIABLES_INPUT}) -- the same way the automation-side {@code ProjectWorkflowFacadeImpl}
 * already does. Before this test existed, the embedded save path called only {@code validateNoDuplicateNodeNames}, so a
 * workflow saved directly through the API (bypassing the shared client-side guard used by both editors) could carry an
 * input or node named {@code vars} that would later be silently clobbered by the platform-seeded job input.
 *
 * <p>
 * Uses the real {@link WorkflowValidatorFacade} default methods (only
 * {@link WorkflowValidatorFacade#getDuplicateNodeNames} needs a minimal fake implementation, mirroring
 * {@code ProjectWorkflowFacadeUpdateWorkflowValidationTest}), so the reserved-name matching exercised here is the
 * actual production logic, not a re-implementation of it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class IntegrationWorkflowFacadeReservedNameValidationTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Mock
    private IntegrationService integrationService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private WorkflowCacheManager workflowCacheManager;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowFacade workflowFacade;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private IntegrationWorkflowFacadeImpl integrationWorkflowFacade;

    @BeforeEach
    void setUp() {
        integrationWorkflowFacade = new IntegrationWorkflowFacadeImpl(
            environmentService, integrationInstanceConfigurationService,
            integrationInstanceConfigurationWorkflowService, integrationService, integrationWorkflowService,
            workflowCacheManager, workflowService, workflowFacade, noDuplicatesWorkflowValidatorFacade(),
            workflowTestConfigurationService, List.of());
    }

    @Test
    void addWorkflowRejectsInputNamedVars() {
        String definitionWithReservedInput = """
            {"label":"t","inputs":[{"name":"vars","type":"STRING"}],"triggers":[],"tasks":[]}
            """;

        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(1L, definitionWithReservedInput))
            .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(integrationService, workflowService);
    }

    @Test
    void addWorkflowRejectsNodeNamedVars() {
        String definitionWithReservedNode = """
            {"label":"t","triggers":[],"tasks":[{"name":"vars","type":"logger/v1/info"}]}
            """;

        assertThatThrownBy(() -> integrationWorkflowFacade.addWorkflow(1L, definitionWithReservedNode))
            .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(integrationService, workflowService);
    }

    @Test
    void addWorkflowAcceptsInputNamedVarsCount() {
        String definitionWithSimilarInput = """
            {"label":"t","inputs":[{"name":"varsCount","type":"STRING"}],"triggers":[],"tasks":[]}
            """;

        Integration integration = new Integration();
        Workflow workflow = mock(Workflow.class);
        IntegrationWorkflow integrationWorkflow = new IntegrationWorkflow(10L);

        when(workflow.getId()).thenReturn("wf-1");
        when(integrationService.getIntegration(1L)).thenReturn(integration);
        when(workflowService.create(definitionWithSimilarInput, Workflow.Format.JSON, Workflow.SourceType.JDBC))
            .thenReturn(workflow);
        when(integrationWorkflowService.addWorkflow(1L, integration.getLastIntegrationVersion(), "wf-1"))
            .thenReturn(integrationWorkflow);

        assertThatCode(() -> integrationWorkflowFacade.addWorkflow(1L, definitionWithSimilarInput))
            .doesNotThrowAnyException();
    }

    @Test
    void updateWorkflowRejectsInputNamedVars() {
        String definitionWithReservedInput = """
            {"label":"t","inputs":[{"name":"vars","type":"STRING"}],"triggers":[],"tasks":[]}
            """;

        assertThatThrownBy(() -> integrationWorkflowFacade.updateWorkflow("wf-1", definitionWithReservedInput, 0))
            .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(workflowFacade);
    }

    @Test
    void updateWorkflowRejectsNodeNamedVars() {
        String definitionWithReservedNode = """
            {"label":"t","triggers":[],"tasks":[{"name":"vars","type":"logger/v1/info"}]}
            """;

        assertThatThrownBy(() -> integrationWorkflowFacade.updateWorkflow("wf-1", definitionWithReservedNode, 0))
            .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(workflowFacade);
    }

    /**
     * A {@link WorkflowValidatorFacade} whose duplicate-name detection always reports no duplicates -- these tests only
     * need to exercise the reserved-name guards, which are real default methods inherited unmodified.
     */
    private static WorkflowValidatorFacade noDuplicatesWorkflowValidatorFacade() {
        return new WorkflowValidatorFacade() {

            @Override
            public WorkflowValidationResult validateWorkflow(String workflow) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public WorkflowValidationResult validateWorkflowById(String workflowId) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public List<String> getDuplicateNodeNames(String workflow) {
                return List.of();
            }
        };
    }
}
