/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CarriedInputUtilsTest {

    private static final String SOURCE_DEFINITION = """
        {
          "label": "Workflow",
          "inputs": [
            {"name": "channel", "type": "string", "required": false},
            {"name": "region", "type": "string", "required": false},
            {"name": "removed", "type": "string", "required": false}
          ],
          "tasks": []
        }
        """;

    private static final String TARGET_DEFINITION = """
        {
          "label": "Workflow",
          "inputs": [
            {"name": "channel", "type": "integer", "required": false},
            {"name": "region", "type": "STRING", "required": false},
            {"name": "added", "type": "string", "required": false}
          ],
          "tasks": []
        }
        """;

    @Test
    void testRetainInputsOfUnchangedTypeDropsRetypedAndRemovedInputs() {
        Map<String, Object> inputs = Map.of("channel", "general", "region", "eu", "removed", "value");

        assertEquals(
            Map.of("region", "eu"),
            CarriedInputUtils.retainInputsOfUnchangedType(
                new Workflow(SOURCE_DEFINITION, Workflow.Format.JSON),
                new Workflow(TARGET_DEFINITION, Workflow.Format.JSON), inputs));
    }

    @Test
    void testRetainInputsOfUnchangedTypeDropsAnInputTheSourceDoesNotDefine() {
        Map<String, Object> inputs = Map.of("added", "value", "region", "eu");

        assertEquals(
            Map.of("region", "eu"),
            CarriedInputUtils.retainInputsOfUnchangedType(
                new Workflow(SOURCE_DEFINITION, Workflow.Format.JSON),
                new Workflow(TARGET_DEFINITION, Workflow.Format.JSON), inputs));
    }
}
