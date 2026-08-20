/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.web.graphql.WorkflowDescriptionCopilotGraphQlController.GenerateWorkflowDescriptionInput;
import com.bytechef.ee.ai.copilot.web.graphql.WorkflowDescriptionCopilotGraphQlController.GenerateWorkflowDescriptionPayload;
import com.bytechef.ee.ai.copilot.web.graphql.facade.WorkflowDescriptionCopilotFacade;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Pins that the mutation reaches the generator through the facade layer, which is where this codebase puts
 * authorization. The {@code WORKFLOW_VIEW} guard itself is pinned by
 * {@code WorkflowDescriptionCopilotFacadeImplTest#testGenerateDeniedWhenUserLacksWorkflowViewScope}.
 *
 * <p>
 * The controller carries no gate of its own and is not meant to. Asserting the delegation is not enough on its own:
 * {@code testControllerHoldsNoAuthorizationCollaborators} is what makes a revert to a locally written
 * {@code permissionService} check fail here rather than pass, since such a check has to reintroduce one of those
 * collaborators as a field.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowDescriptionCopilotGraphQlControllerTest {

    private final WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade =
        mock(WorkflowDescriptionCopilotFacade.class);

    private final WorkflowDescriptionCopilotGraphQlController controller =
        new WorkflowDescriptionCopilotGraphQlController(workflowDescriptionCopilotFacade);

    @Test
    void testGenerateWorkflowDescriptionDelegatesToTheGuardedFacade() {
        when(workflowDescriptionCopilotFacade.generateWorkflowDescription(request()))
            .thenReturn(new WorkflowDescriptionCopilotResult("Syncs records."));

        GenerateWorkflowDescriptionPayload payload = controller.generateWorkflowDescription(input());

        assertThat(payload.value()).isEqualTo("Syncs records.");

        verify(workflowDescriptionCopilotFacade).generateWorkflowDescription(request());
    }

    @Test
    void testControllerHoldsNoAuthorizationCollaborators() {
        assertThat(Arrays.stream(WorkflowDescriptionCopilotGraphQlController.class.getDeclaredFields())
            .map(Field::getType))
                .as("authorization belongs on the facade; the controller must not hold the services a gate needs")
                .doesNotContain(PermissionService.class, ProjectWorkflowService.class);
    }

    private static GenerateWorkflowDescriptionInput input() {
        return new GenerateWorkflowDescriptionInput("wf1", null, 0);
    }

    private static WorkflowDescriptionCopilotRequest request() {
        return new WorkflowDescriptionCopilotRequest("wf1", null, 0);
    }
}
