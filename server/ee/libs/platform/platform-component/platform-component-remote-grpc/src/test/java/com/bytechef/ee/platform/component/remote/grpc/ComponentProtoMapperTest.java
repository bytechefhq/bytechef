/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.AiAgentComponentHandler;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.schedule.ScheduleComponentHandler;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.scheduler.TriggerScheduler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.tool.ToolCallingManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentProtoMapperTest {

    private static final String FREE_FORM_FIELDS = ".*(sampleOutput|placeholder|defaultValue|exampleValue|value)";

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final ComponentProtoMapper componentProtoMapper = new ComponentProtoMapper(jsonMapper);

    @Test
    void testComponentDefinitionsRoundTrip() {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions();

        assertThat(componentDefinitions).extracting(ComponentDefinition::getName)
            .contains("aiAgent", "dataMapper", "httpClient", "schedule", "webhook");

        for (ComponentDefinition componentDefinition : componentDefinitions) {
            ComponentDefinition roundTripComponentDefinition = componentProtoMapper.toComponentDefinition(
                componentProtoMapper.toProto(componentDefinition));

            assertThat(roundTripComponentDefinition)
                .as(componentDefinition.getName())
                .usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(this::jsonEquals, FREE_FORM_FIELDS)
                .isEqualTo(componentDefinition);
        }
    }

    @Test
    void testFacadeResultsRoundTrip() {
        OutputResponse outputResponse = getWebhookTriggerOutputResponse();

        assertThat(componentProtoMapper.toOutputResponse(componentProtoMapper.toProto(outputResponse)))
            .usingRecursiveComparison()
            .withEqualsForFieldsMatchingRegexes(this::jsonEquals, FREE_FORM_FIELDS)
            .isEqualTo(outputResponse);

        TriggerOutput triggerOutput = new TriggerOutput(Map.of("count", 1), Map.of("cursor", "abc"), true);

        assertThat(componentProtoMapper.toTriggerOutput(componentProtoMapper.toProto(triggerOutput)))
            .isEqualTo(triggerOutput);

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "hook-1"), Instant.parse("2026-10-26T10:15:30Z"));

        assertThat(componentProtoMapper.toWebhookEnableOutput(componentProtoMapper.toProto(webhookEnableOutput)))
            .isEqualTo(webhookEnableOutput);

        WebhookValidateResponse webhookValidateResponse = new WebhookValidateResponse(
            Map.of("ok", true), Map.of("X-Hook", List.of("a", "b")), 202);

        assertThat(
            componentProtoMapper.toWebhookValidateResponse(componentProtoMapper.toProto(webhookValidateResponse)))
                .isEqualTo(webhookValidateResponse);

        WebhookRequest webhookRequest = new WebhookRequest(
            Map.of("Content-Type", List.of("application/json")), Map.of("q", List.of("1")),
            new WebhookRequest.WebhookBodyImpl(
                Map.of("name", "x"), ContentType.JSON, "application/json", "{\"name\":\"x\"}"),
            WebhookMethod.POST, true);

        assertThat(componentProtoMapper.toWebhookRequest(componentProtoMapper.toProto(webhookRequest)))
            .usingRecursiveComparison()
            .isEqualTo(webhookRequest);
    }

    private boolean jsonEquals(Object actual, Object expected) {
        return Objects.equals(jsonMapper.writeValueAsString(actual), jsonMapper.writeValueAsString(expected));
    }

    private static List<ComponentDefinition> getComponentDefinitions() {
        List<ComponentDefinition> componentDefinitions = new ArrayList<>();

        for (ComponentHandler componentHandler : ServiceLoader.load(ComponentHandler.class)) {
            componentDefinitions.add(new ComponentDefinition(componentHandler.getDefinition()));
        }

        ComponentHandler aiAgentComponentHandler = new AiAgentComponentHandler(
            mock(AiAgentToolFacade.class), mock(ClusterElementDefinitionService.class), mock(ToolCallingManager.class));
        ComponentHandler scheduleComponentHandler = new ScheduleComponentHandler(mock(TriggerScheduler.class));

        componentDefinitions.add(new ComponentDefinition(aiAgentComponentHandler.getDefinition()));
        componentDefinitions.add(new ComponentDefinition(scheduleComponentHandler.getDefinition()));

        return componentDefinitions;
    }

    private static OutputResponse getWebhookTriggerOutputResponse() {
        return getComponentDefinitions().stream()
            .flatMap(componentDefinition -> componentDefinition.getTriggers()
                .stream())
            .map(TriggerDefinition::getOutputResponse)
            .filter(curOutputResponse -> curOutputResponse != null && curOutputResponse.outputSchema() != null)
            .findFirst()
            .orElseThrow();
    }
}
