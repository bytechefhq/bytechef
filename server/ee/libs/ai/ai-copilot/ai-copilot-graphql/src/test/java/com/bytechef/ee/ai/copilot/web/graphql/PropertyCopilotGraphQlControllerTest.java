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
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.ee.ai.copilot.web.graphql.PropertyCopilotGraphQlController.GeneratePropertyValueInput;
import com.bytechef.ee.ai.copilot.web.graphql.PropertyCopilotGraphQlController.GeneratePropertyValuePayload;
import com.bytechef.ee.ai.copilot.web.graphql.facade.PropertyCopilotFacade;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Pins that the mutation reaches the generator through the facade layer, which is where this codebase puts
 * authorization. The {@code WORKFLOW_VIEW} guard itself is pinned by
 * {@code PropertyCopilotFacadeImplTest#testGenerateDeniedWhenUserLacksWorkflowViewScope}.
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
class PropertyCopilotGraphQlControllerTest {

    private final PropertyCopilotFacade propertyCopilotFacade = mock(PropertyCopilotFacade.class);

    private final PropertyCopilotGraphQlController controller =
        new PropertyCopilotGraphQlController(propertyCopilotFacade);

    @Test
    void testGeneratePropertyValueDelegatesToTheGuardedFacade() {
        when(propertyCopilotFacade.generatePropertyValue(request()))
            .thenReturn(new PropertyCopilotResult("Hello", true, null));

        GeneratePropertyValuePayload payload = controller.generatePropertyValue(input());

        assertThat(payload.value()).isEqualTo("Hello");
        assertThat(payload.valid()).isTrue();

        verify(propertyCopilotFacade).generatePropertyValue(request());
    }

    @Test
    void testControllerHoldsNoAuthorizationCollaborators() {
        assertThat(Arrays.stream(PropertyCopilotGraphQlController.class.getDeclaredFields())
            .map(Field::getType))
                .as("authorization belongs on the facade; the controller must not hold the services a gate needs")
                .doesNotContain(PermissionService.class, ProjectWorkflowService.class);
    }

    private static GeneratePropertyValueInput input() {
        return new GeneratePropertyValueInput("greet", PropertyCopilotMode.TEXT, "wf1", "n1", "p", "STRING", true, 0);
    }

    private static PropertyCopilotRequest request() {
        return new PropertyCopilotRequest("greet", PropertyCopilotMode.TEXT, "wf1", "n1", "p", "STRING", true, 0);
    }
}
