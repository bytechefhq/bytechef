/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ConnectedUserWorkflowWebhookUrlResolverTest {

    private static final long PROJECT_DEPLOYMENT_ID = 900L;
    private static final String WORKFLOW_ID = "workflow-1";
    private static final String WORKFLOW_UUID = "be530417-2226-41d4-aae9-c4fc76f0791a";

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WorkflowService workflowService;

    private ConnectedUserWorkflowWebhookUrlResolver resolver;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setWebhookUrl("https://bytechef.example.com/webhooks/{id}");

        resolver = new ConnectedUserWorkflowWebhookUrlResolver(
            applicationProperties, projectWorkflowService, triggerDefinitionService, workflowService);
    }

    @Test
    void testResolveReturnsTheWebhookUrlOfAStaticWebhookTrigger() {
        mockDeployedWorkflow("request/v1/awaitWorkflowAndRespond");
        mockTriggerDefinition("request", "awaitWorkflowAndRespond", TriggerType.STATIC_WEBHOOK);

        String staticWebhookUrl = resolver.resolve(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID, "trigger_1");

        assertThat(staticWebhookUrl).isEqualTo("https://bytechef.example.com/webhooks/" + workflowExecutionId);
    }

    @Test
    void testResolveReturnsNullForATriggerThatIsNotAStaticWebhook() {
        mockDeployedWorkflow("infobip/v1/newWhatsappMessage");
        mockTriggerDefinition("infobip", "newWhatsappMessage", TriggerType.DYNAMIC_WEBHOOK);

        assertThat(resolver.resolve(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID)).isNull();
    }

    @Test
    void testResolveReturnsNullWhenTheTriggerDefinitionCannotBeLoaded() {
        mockDeployedWorkflow("request/v1/awaitWorkflowAndRespond");

        when(triggerDefinitionService.getTriggerDefinition("request", 1, "awaitWorkflowAndRespond"))
            .thenThrow(new IllegalArgumentException("unknown trigger"));

        assertThat(resolver.resolve(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID)).isNull();
    }

    @Test
    void testResolveReturnsNullWhenTheWorkflowIsNotDeployed() {
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.empty());

        assertThat(resolver.resolve(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID)).isNull();

        verifyNoInteractions(workflowService);
    }

    @Test
    void testResolveReturnsNullWithoutADeployment() {
        assertThat(resolver.resolve(null, WORKFLOW_UUID)).isNull();

        verifyNoInteractions(projectWorkflowService);
    }

    private void mockDeployedWorkflow(String triggerType) {
        String definition =
            "{\"label\": \"Workflow\", \"triggers\": [{\"name\": \"trigger_1\", \"type\": \"" + triggerType +
                "\"}], \"tasks\": []}";

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(PROJECT_DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of(WORKFLOW_ID));
        when(workflowService.getWorkflow(WORKFLOW_ID))
            .thenReturn(new Workflow(WORKFLOW_ID, definition, Workflow.Format.JSON));
    }

    private void mockTriggerDefinition(String componentName, String triggerName, TriggerType triggerType) {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getType()).thenReturn(triggerType);
        when(triggerDefinitionService.getTriggerDefinition(componentName, 1, triggerName))
            .thenReturn(triggerDefinition);
    }
}
