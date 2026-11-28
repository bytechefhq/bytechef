/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowDefinitionsTest {

    private static final String YAML_DEFINITION = """
        label: Orders
        tasks:
          - name: http_1
            type: httpClient/v1/get
            parameters:
              uri: https://a.example
        """;

    @Test
    void testSameDefinitionIgnoresKeyOrderAndWhitespace() {
        String left = "{\"label\": \"Orders\", \"tasks\": [{\"name\": \"http_1\", \"type\": \"httpClient/v1/get\"}]}";
        String right = """
            {
              "tasks": [
                {"type": "httpClient/v1/get", "name": "http_1"}
              ],
              "label": "Orders"
            }
            """;

        assertThat(WorkflowDefinitions.sameDefinition(left, Format.JSON, right, Format.JSON)).isTrue();
    }

    @Test
    void testDifferentTaskParameterIsDifferent() {
        String left = "{\"label\": \"Orders\", \"tasks\": [{\"name\": \"http_1\", \"type\": \"httpClient/v1/get\", " +
            "\"parameters\": {\"uri\": \"https://a.example\"}}]}";
        String right = "{\"label\": \"Orders\", \"tasks\": [{\"name\": \"http_1\", \"type\": \"httpClient/v1/get\", " +
            "\"parameters\": {\"uri\": \"https://b.example\"}}]}";

        assertThat(WorkflowDefinitions.sameDefinition(left, Format.JSON, right, Format.JSON)).isFalse();
    }

    @Test
    void testSameYamlDefinitionIgnoresKeyOrderAndWhitespace() {
        String reorderedYamlDefinition = """
            tasks:
              -   type: httpClient/v1/get
                  parameters: {uri: "https://a.example"}
                  name: http_1

            label: "Orders"
            """;

        assertThat(
            WorkflowDefinitions.sameDefinition(
                new Workflow("orders-1", YAML_DEFINITION, Format.YAML),
                new Workflow("orders-2", reorderedYamlDefinition, Format.YAML)))
                    .isTrue();
    }

    @Test
    void testDifferentYamlTaskParameterIsDifferent() {
        String changedYamlDefinition = YAML_DEFINITION.replace("https://a.example", "https://b.example");

        assertThat(
            WorkflowDefinitions.sameDefinition(
                new Workflow("orders-1", YAML_DEFINITION, Format.YAML),
                new Workflow("orders-2", changedYamlDefinition, Format.YAML)))
                    .isFalse();
    }

    @Test
    void testJsonAndYamlWithSameContentAreSame() {
        String jsonDefinition = "{\"label\": \"Orders\", \"tasks\": [{\"name\": \"http_1\", " +
            "\"type\": \"httpClient/v1/get\", \"parameters\": {\"uri\": \"https://a.example\"}}]}";

        assertThat(WorkflowDefinitions.sameDefinition(jsonDefinition, Format.JSON, YAML_DEFINITION, Format.YAML))
            .isTrue();
    }
}
