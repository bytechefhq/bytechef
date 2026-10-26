/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PlatformConfigurationProtoMapperTest {

    static final String DEFINITION = """
        {"label":"Live Workflow","tasks":[
            {"name":"randomHelper_1","type":"randomHelper/v1/randomFloat","parameters":{"from":1,"to":100}},
            {"name":"logger_1","type":"logger/v1/info","parameters":{"text":"${randomHelper_1}"}}]}
        """;

    private final PlatformConfigurationProtoMapper platformConfigurationProtoMapper =
        new PlatformConfigurationProtoMapper(
            JsonMapper.builder()
                .build());

    @Test
    void testWorkflowRoundTrip() {
        Workflow workflow = workflow("3e4078b4-87bb-43c0-9cc6-1270f73b131d");

        Workflow readWorkflow = platformConfigurationProtoMapper.toWorkflow(
            platformConfigurationProtoMapper.toProto(workflow));

        assertThat(readWorkflow.getId()).isEqualTo(workflow.getId());
        assertThat(readWorkflow.getDefinition()).isEqualTo(DEFINITION);
        assertThat(readWorkflow.getFormat()).isEqualTo(Workflow.Format.JSON);
        assertThat(readWorkflow.getLastModifiedDate()).isEqualTo(workflow.getLastModifiedDate());
        assertThat(readWorkflow.getMetadata()).isEqualTo(Map.of("id", 9_007_199_254_740_993L));
        assertThat(readWorkflow.getSourceType()).isEqualTo(Workflow.SourceType.JDBC);
        assertThat(readWorkflow.getVersion()).isEqualTo(3);
        assertThat(readWorkflow.isNew()).isFalse();
        assertThat(readWorkflow.getTasks()).extracting(WorkflowTask::toMap)
            .containsExactlyElementsOf(
                workflow.getTasks()
                    .stream()
                    .map(WorkflowTask::toMap)
                    .toList());
    }

    @Test
    void testComponentConnectionRoundTrip() {
        List<ComponentConnection> componentConnections = List.of(
            new ComponentConnection("airtable", 1, "airtable_1", "airtable", true),
            new ComponentConnection("httpClient", 1, "httpClient_1", "httpClient", false));

        assertThat(platformConfigurationProtoMapper.toComponentConnections(
            platformConfigurationProtoMapper.toRemoteComponentConnections(componentConnections)))
                .isEqualTo(componentConnections);
    }

    static Workflow workflow(String id) {
        Workflow workflow = new Workflow(
            id, DEFINITION, Workflow.Format.JSON, Instant.parse("2026-10-26T11:00:00Z"),
            Map.of("id", 9_007_199_254_740_993L));

        workflow.setSourceType(Workflow.SourceType.JDBC);
        workflow.setVersion(3);

        return workflow;
    }
}
