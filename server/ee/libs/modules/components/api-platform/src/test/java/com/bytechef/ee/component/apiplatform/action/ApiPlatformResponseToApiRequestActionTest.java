/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.action;

import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.FORBIDDEN;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INTERNAL_ERROR;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INVALID_INPUT;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE_TYPE;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.ee.component.apiplatform.constant.ResponseType;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ApiPlatformResponseToApiRequestActionTest {

    private final ApiPlatformResponseToApiRequestAction apiPlatformResponseToApiRequestAction =
        new ApiPlatformResponseToApiRequestAction(null);

    @Test
    void testPerformSuccessReturnsOk() {
        assertStatusCode(ResponseType.SUCCESS, SUCCESS, HttpStatus.OK);
    }

    @Test
    void testPerformInvalidInputReturnsBadRequest() {
        assertStatusCode(ResponseType.INVALID_INPUT, INVALID_INPUT, HttpStatus.BAD_REQUEST);
    }

    @Test
    void testPerformInternalErrorReturnsInternalServerError() {
        assertStatusCode(ResponseType.INTERNAL_ERROR, INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testPerformForbiddenReturnsForbidden() {
        assertStatusCode(ResponseType.FORBIDDEN, FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    @Test
    void testUsesFirstEnabledTrigger() {
        WorkflowService workflowService = mock(WorkflowService.class);
        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);
        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), any()))
            .thenReturn(
                List.of(
                    new WorkflowTrigger(
                        Map.of(
                            "name", "trigger_1", "type", "webhook/v1/autoRespondingWithHTTP200", "disabled", true)),
                    new WorkflowTrigger(
                        Map.of(
                            "name", "trigger_2", "type", "apiPlatform/v1/newApiRequest", "parameters",
                            Map.of(RESPONSE, Map.of(SUCCESS, "{\"type\": \"string\"}"))))));

        ActionContext actionContext =
            mock(ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        when(((ActionContextAware) actionContext).getWorkflowId()).thenReturn("workflow-1");

        ApiPlatformResponseToApiRequestAction action = new ApiPlatformResponseToApiRequestAction(workflowService);

        Parameters inputParameters = MockParametersFactory.create(Map.of(RESPONSE_TYPE, ResponseType.SUCCESS.name()));

        action.responseProperties(inputParameters, null, Map.of(), actionContext);

        verify(actionContext).outputSchema(any());
    }

    private void assertStatusCode(ResponseType responseType, String responseKey, HttpStatus expectedStatus) {
        Map<String, Object> body = Map.of("message", "value");

        Parameters inputParameters = MockParametersFactory.create(
            Map.of(RESPONSE_TYPE, responseType.name(), RESPONSE, Map.of(responseKey, body)));

        WebhookResponse webhookResponse = apiPlatformResponseToApiRequestAction.perform(inputParameters, null, null);

        assertThat(webhookResponse.getStatusCode()).isEqualTo(expectedStatus.getValue());
        assertThat(webhookResponse.getBody()).isEqualTo(body);
    }
}
