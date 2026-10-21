/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.workflow.CallableWorkflowCheck;
import com.bytechef.automation.configuration.workflow.WorkflowKind;
import com.bytechef.automation.configuration.workflow.WorkflowKindChecker;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApiEndpointWorkflowCheckerTest {

    private static final String BODY_SCHEMA =
        "{\"type\": \"object\", \"properties\": {\"orderId\": {\"type\": \"string\"}}}";
    private static final String RESPONSE_TASK =
        "{\"name\": \"apiPlatform_1\", \"type\": \"apiPlatform/v1/responseToAPIRequest\", " +
            "\"parameters\": {\"responseType\": \"SUCCESS\", \"response\": {\"success\": {\"ok\": true}}}}";

    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final ApiEndpointWorkflowChecker apiEndpointWorkflowChecker = new ApiEndpointWorkflowChecker(
        projectWorkflowService, workflowService);

    @Test
    void testKindIsApiEndpoint() {
        assertThat(apiEndpointWorkflowChecker.kind()).isEqualTo(WorkflowKind.API_ENDPOINT);
    }

    @Test
    void testANewApiRequestTriggerWithReadableSchemasAndAResponsePasses() {
        stubWorkflow(
            "apiPlatform/v1/newApiRequest",
            Map.of("request", Map.of("body", BODY_SCHEMA, "headers", "{\"type\": \"object\"}"),
                "response", Map.of("success", "{\"type\": \"object\"}")),
            RESPONSE_TASK);

        CallableWorkflowCheck callableWorkflowCheck = apiEndpointWorkflowChecker.check("wf-1");

        assertThat(callableWorkflowCheck.callable()).isTrue();
        assertThat(callableWorkflowCheck.problems()).isEmpty();
    }

    @Test
    void testARequestWithoutSchemasPasses() {
        stubWorkflow("apiPlatform/v1/newApiRequest", Map.of("request", Map.of()), RESPONSE_TASK);

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .callable()).isTrue();
    }

    @Test
    void testATriggerWithoutARequestIsReported() {
        stubWorkflow("apiPlatform/v1/newApiRequest", Map.of(), RESPONSE_TASK);

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("trigger has no request parameter");
    }

    @Test
    void testAResponseTaskWithoutAResponseIsReported() {
        stubWorkflow(
            "apiPlatform/v1/newApiRequest", Map.of("request", Map.of("body", BODY_SCHEMA)),
            "{\"name\": \"apiPlatform_1\", \"type\": \"apiPlatform/v1/responseToAPIRequest\", " +
                "\"parameters\": {\"responseType\": \"SUCCESS\"}}");

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("responseToAPIRequest task apiPlatform_1 has no response parameter");
    }

    @Test
    void testAnotherTriggerIsNamed() {
        stubWorkflow("manual/v1/manual", Map.of(), RESPONSE_TASK);

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("trigger is manual/v1/manual, not apiPlatform/v1/newApiRequest");
    }

    @Test
    void testAWorkflowWithoutATriggerIsNamed() {
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(
            Optional.of(new Workflow("wf-1", "{\"tasks\": [" + RESPONSE_TASK + "]}", Workflow.Format.JSON)));

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("trigger is none, not apiPlatform/v1/newApiRequest");
    }

    @Test
    void testAMissingResponseTaskIsReported() {
        stubWorkflow(
            "apiPlatform/v1/newApiRequest", Map.of("request", Map.of("body", BODY_SCHEMA)),
            "{\"name\": \"logger_1\", \"type\": \"logger/v1/info\", \"parameters\": {}}");

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("no responseToAPIRequest task");
    }

    @Test
    void testAnUnparsableRequestSchemaIsReported() {
        stubWorkflow("apiPlatform/v1/newApiRequest", Map.of("request", Map.of("body", "{not json")), RESPONSE_TASK);

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("request schema unreadable");
    }

    @Test
    void testANonStringRequestSchemaIsReported() {
        stubWorkflow(
            "apiPlatform/v1/newApiRequest", Map.of("request", Map.of("parameters", Map.of("type", "object"))),
            RESPONSE_TASK);

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("request schema unreadable");
    }

    @Test
    void testANonObjectRequestIsReported() {
        stubWorkflow("apiPlatform/v1/newApiRequest", Map.of("request", "body"), RESPONSE_TASK);

        assertThat(apiEndpointWorkflowChecker.check("wf-1")
            .problems()).containsExactly("request schema unreadable");
    }

    @Test
    void testAMissingWorkflowIsNamed() {
        when(workflowService.fetchWorkflow("wf-9")).thenReturn(Optional.empty());

        assertThat(apiEndpointWorkflowChecker.check("wf-9")
            .problems()).containsExactly("workflow wf-9 does not exist");
    }

    @Test
    void testFetchWorkflowUuidReturnsTheProjectWorkflowUuid() {
        UUID uuid = UUID.fromString("0b9f0b0e-5f1e-4d0a-8c52-2c3a0a8d8b11");

        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1"))
            .thenReturn(Optional.of(new ProjectWorkflow(1L, 1, "wf-1", uuid)));

        assertThat(apiEndpointWorkflowChecker.fetchWorkflowUuid("wf-1")).contains(uuid.toString());
    }

    @Nested
    class EditionGating {

        private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withBean(ProjectWorkflowService.class, () -> mock(ProjectWorkflowService.class))
            .withBean(WorkflowService.class, () -> mock(WorkflowService.class))
            .withUserConfiguration(ApiEndpointWorkflowChecker.class);

        @Test
        void testCeEditionDoesNotOfferApiEndpoint() {
            applicationContextRunner.withPropertyValues("bytechef.edition=ce")
                .run(context -> assertThat(context.getBeansOfType(WorkflowKindChecker.class)).isEmpty());
        }

        @Test
        void testAbsentEditionDoesNotOfferApiEndpoint() {
            applicationContextRunner.run(
                context -> assertThat(context.getBeansOfType(WorkflowKindChecker.class)).isEmpty());
        }

        @Test
        void testEeEditionOffersApiEndpoint() {
            applicationContextRunner.withPropertyValues("bytechef.edition=ee")
                .run(context -> assertThat(context.getBeansOfType(WorkflowKindChecker.class)
                    .values())
                        .extracting(WorkflowKindChecker::kind)
                        .containsExactly(WorkflowKind.API_ENDPOINT));
        }
    }

    private void stubWorkflow(String triggerType, Map<String, ?> triggerParameters, String task) {
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(
            Optional.of(
                new Workflow(
                    "wf-1",
                    """
                        {"triggers": [{"name": "trigger_1", "type": "%s", "parameters": %s}], "tasks": [%s]}\
                        """.formatted(triggerType, JsonUtils.write(triggerParameters), task),
                    Workflow.Format.JSON)));
    }
}
