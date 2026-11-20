/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class InternalOnlyInputUtilsTest {

    private static final String DEFINITION = """
        {
          "label": "Workflow",
          "inputs": [
            {"name": "accountId", "type": "string", "required": true, "internalOnly": true},
            {"name": "region", "type": "string", "required": false, "internalOnly": true},
            {"name": "channel", "type": "string", "required": true}
          ],
          "tasks": []
        }
        """;

    @Test
    void testGetInternalOnlyInputNames() {
        assertEquals(Set.of("accountId", "region"), InternalOnlyInputUtils.getInternalOnlyInputNames(workflow()));
    }

    @Test
    void testRemoveInternalOnlyInputs() {
        Map<String, Object> inputs = new LinkedHashMap<>();

        inputs.put("accountId", "a");
        inputs.put("region", "r");
        inputs.put("channel", "c");

        assertEquals(Map.of("channel", "c"), InternalOnlyInputUtils.removeInternalOnlyInputs(workflow(), inputs));
    }

    @Test
    void testFindMissingRequiredInternalOnlyInputReturnsTheName() {
        assertEquals("accountId", InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(workflow(), Map.of()));
    }

    @Test
    void testFindMissingRequiredInternalOnlyInputTreatsBlankAsMissing() {
        assertEquals(
            "accountId",
            InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(workflow(), Map.of("accountId", "  ")));
    }

    @Test
    void testFindMissingRequiredInternalOnlyInputIgnoresOptionalAndPlain() {
        assertNull(InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(workflow(), Map.of("accountId", "a")));
    }

    @Test
    void testRemoveInternalOnlyInputsFromDefinition() {
        String rewrittenDefinition = InternalOnlyInputUtils.removeInternalOnlyInputs(workflow());

        List<?> inputs = JsonUtils.readList(rewrittenDefinition, "inputs");

        assertEquals(1, inputs.size());

        Map<?, ?> input = (Map<?, ?>) inputs.get(0);

        assertEquals("channel", input.get("name"));
    }

    @Test
    void testRemoveInternalOnlyInputsFromDefinitionWithoutInputsReturnsItUnchanged() {
        String definition = "{\"label\": \"Workflow\", \"tasks\": []}";

        assertEquals(
            definition,
            InternalOnlyInputUtils.removeInternalOnlyInputs(new Workflow(definition, Workflow.Format.JSON)));
    }

    @Test
    void testRemoveInternalOnlyInputsFromDefinitionWithoutInternalOnlyInputsReturnsItUnchanged() {
        String definition = """
            {
              "label": "Workflow",
              "inputs": [{"name": "channel", "type": "string", "required": true, "internalOnly": false}],
              "tasks": []
            }
            """;

        assertEquals(
            definition,
            InternalOnlyInputUtils.removeInternalOnlyInputs(new Workflow(definition, Workflow.Format.JSON)));
    }

    @Test
    void testRemoveInternalOnlyInputsFromDefinitionMatchesAStringFlag() {
        String definition = """
            {
              "label": "Workflow",
              "inputs": [
                {"name": "accountId", "type": "string", "internalOnly": "true"},
                {"name": "channel", "type": "string"}
              ],
              "tasks": []
            }
            """;

        String rewrittenDefinition = InternalOnlyInputUtils.removeInternalOnlyInputs(
            new Workflow(definition, Workflow.Format.JSON));

        assertEquals(List.of("channel"), inputNames(rewrittenDefinition));
    }

    @Test
    void testRemoveInternalOnlyInputsFromYamlDefinition() {
        String definition = """
            label: Workflow
            inputs:
              - name: accountId
                type: string
                internalOnly: true
              - name: channel
                type: string
            tasks:
              - name: postMessage1
                type: slack/v1/postMessage
            """;

        String rewrittenDefinition = InternalOnlyInputUtils.removeInternalOnlyInputs(
            new Workflow(definition, Workflow.Format.YAML));

        assertEquals(List.of("channel"), inputNames(rewrittenDefinition));

        List<?> tasks = JsonUtils.readList(rewrittenDefinition, "tasks");

        assertEquals(List.of(Map.of("name", "postMessage1", "type", "slack/v1/postMessage")), tasks);
    }

    @Test
    void testRemoveInternalOnlyInputsFromYamlDefinitionWithoutInternalOnlyInputsReturnsItUnchanged() {
        String definition = """
            label: Workflow
            inputs:
              - name: channel
                type: string
            tasks: []
            """;

        assertEquals(
            definition,
            InternalOnlyInputUtils.removeInternalOnlyInputs(new Workflow(definition, Workflow.Format.YAML)));
    }

    private static List<?> inputNames(String definition) {
        return JsonUtils.readList(definition, "inputs")
            .stream()
            .map(input -> ((Map<?, ?>) input).get("name"))
            .toList();
    }

    private static Workflow workflow() {
        return new Workflow(DEFINITION, Workflow.Format.JSON);
    }
}
