/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppWorkflowSchemasTest {

    private static final String SCHEMA = """
        {"type": "object",
         "properties": {"orderId": {"type": "string"}, "amount": {"type": "number"}},
         "required": ["orderId"]}
        """;

    @Test
    void testValidInputHasNoErrors() {
        assertThat(AppWorkflowSchemas.validate(SCHEMA, Map.of("orderId", "A-1", "amount", 12.5))).isEmpty();
    }

    @Test
    void testMissingRequiredFieldIsReported() {
        assertThat(AppWorkflowSchemas.validate(SCHEMA, Map.of("amount", 12.5)))
            .singleElement()
            .asString()
            .contains("orderId");
    }

    @Test
    void testWrongTypeIsReported() {
        assertThat(AppWorkflowSchemas.validate(SCHEMA, Map.of("orderId", 7))).isNotEmpty();
    }

    @Test
    void testNoSchemaAcceptsAnyInput() {
        assertThat(AppWorkflowSchemas.validate(null, Map.of("anything", true))).isEmpty();
        assertThat(AppWorkflowSchemas.validate("  ", Map.of("anything", true))).isEmpty();
    }

    @Test
    void testUnparseableSchemaThrows() {
        assertThatThrownBy(() -> AppWorkflowSchemas.validate("{not json", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testParseReturnsTheSchemaAsAMap() {
        assertThat(AppWorkflowSchemas.parse(SCHEMA)).containsEntry("type", "object");
        assertThat(AppWorkflowSchemas.parse(null)).isNull();
    }

    @Test
    void testSchemaWithAnUnresolvableReferenceIsNotReadable() {
        String schema = """
            {"$ref": "#/definitions/missing"}
            """;

        assertThatThrownBy(() -> AppWorkflowSchemas.checkReadable(schema))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AppWorkflowSchemas.validate(schema, Map.of("orderId", "A-1")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSchemaWithAnInvalidPatternIsNotReadable() {
        String schema = """
            {"pattern": "["}
            """;

        assertThatThrownBy(() -> AppWorkflowSchemas.checkReadable(schema))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AppWorkflowSchemas.validate(schema, Map.of("orderId", "A-1")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testReadableSchemaPassesTheCheck() {
        assertThatCode(() -> AppWorkflowSchemas.checkReadable(SCHEMA)).doesNotThrowAnyException();
        assertThatCode(() -> AppWorkflowSchemas.checkReadable(null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> AppWorkflowSchemas.checkReadable("{not json"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
