/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.tool.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ToolParameterDefaultsUtilsTest {

    @Test
    void testCollectPropertyDefaultsSkipsPropertiesWithoutDefault() {
        Map<String, Object> propertyDefaults = ToolParameterDefaultsUtils.collectPropertyDefaults(
            List.of(
                Property.toProperty(integer("length").defaultValue(8)),
                Property.toProperty(string("text")),
                Property.toProperty(bool("flag").defaultValue(false))));

        assertThat(propertyDefaults).isEqualTo(Map.of("length", 8L, "flag", false));
    }

    @Test
    void testCollectPropertyDefaultsRecursesIntoObjectProperties() {
        Map<String, Object> propertyDefaults = ToolParameterDefaultsUtils.collectPropertyDefaults(
            List.of(
                Property.toProperty(
                    object("body").properties(
                        string("content"),
                        string("format").defaultValue("text"),
                        object("options").properties(integer("limit").defaultValue(10)))),
                Property.toProperty(object("empty").properties(string("name")))));

        assertThat(propertyDefaults)
            .isEqualTo(Map.of("body", Map.of("format", "text", "options", Map.of("limit", 10L))));
    }

    @Test
    void testCollectPropertyDefaultsLetsObjectDefaultOverrideSubPropertyDefaults() {
        Map<String, Object> propertyDefaults = ToolParameterDefaultsUtils.collectPropertyDefaults(
            List.of(
                Property.toProperty(
                    object("body")
                        .properties(string("format").defaultValue("text"), string("mode").defaultValue("plain"))
                        .defaultValue(Map.of("format", "html")))));

        assertThat(propertyDefaults).isEqualTo(Map.of("body", Map.of("format", "html", "mode", "plain")));
    }

    @Test
    void testCollectPropertyDefaultsSkipsExpressionDefaults() {
        Map<String, Object> propertyDefaults = ToolParameterDefaultsUtils.collectPropertyDefaults(
            List.of(
                Property.toProperty(string("date").defaultValue("=now()")),
                Property.toProperty(string("greeting").defaultValue("hello")),
                Property.toProperty(object("body").properties(string("expression").defaultValue("=input.value")))));

        assertThat(propertyDefaults).isEqualTo(Map.of("greeting", "hello"));
    }

    @Test
    void testCollectTriggerInputDefaultsReturnsEmptyWithoutInputSchema() {
        assertThat(ToolParameterDefaultsUtils.collectTriggerInputDefaults(Map.of())).isEmpty();
        assertThat(ToolParameterDefaultsUtils.collectTriggerInputDefaults(null)).isEmpty();
    }

    @Test
    void testCollectTriggerInputDefaultsReturnsEmptyWhenSchemaCannotBeParsed() {
        assertThat(
            ToolParameterDefaultsUtils.collectTriggerInputDefaults(
                Map.of(WorkflowConstants.INPUT_SCHEMA, "{\"type\":\"unknown\"}")))
                    .isEmpty();
        assertThat(
            ToolParameterDefaultsUtils.collectTriggerInputDefaults(
                Map.of(WorkflowConstants.INPUT_SCHEMA, "not json")))
                    .isEmpty();
    }

    @Test
    void testCollectTriggerInputDefaultsReturnsEmptyWhenSchemaHasNoDefaults() {
        assertThat(
            ToolParameterDefaultsUtils.collectTriggerInputDefaults(
                Map.of(
                    WorkflowConstants.INPUT_SCHEMA,
                    "{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\"}}}")))
                        .isEmpty();
    }

    @Test
    void testMergeParametersLetsLaterValuesWinAndMergesNestedObjects() {
        Map<String, Object> defaults = Map.of(
            "length", 8, "body", Map.of("content", "default", "format", "text"), "flag", true);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("length", 12);
        parameters.put("body", Map.of("content", "x"));
        parameters.put("flag", null);
        parameters.put("extra", null);

        Map<String, Object> mergedParameters = ToolParameterDefaultsUtils.mergeParameters(defaults, parameters);

        Map<String, Object> expectedParameters = new HashMap<>();

        expectedParameters.put("length", 12);
        expectedParameters.put("body", Map.of("content", "x", "format", "text"));
        expectedParameters.put("flag", true);
        expectedParameters.put("extra", null);

        assertThat(mergedParameters).isEqualTo(expectedParameters);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMergeParametersDoesNotShareNestedDefaultsWithTheMergedResult() {
        Map<String, Object> defaults = Map.of(
            "body", Map.of("format", "text"), "tags", List.of("first"),
            "options", Map.of("limits", List.of(Map.of("size", 1))));

        Map<String, Object> mergedParameters = ToolParameterDefaultsUtils.mergeParameters(defaults, Map.of());

        ((Map<String, Object>) mergedParameters.get("body")).put("format", "html");
        ((List<Object>) mergedParameters.get("tags")).add("second");

        Map<String, Object> options = (Map<String, Object>) mergedParameters.get("options");

        ((Map<String, Object>) ((List<Object>) options.get("limits")).getFirst()).put("size", 2);

        assertThat(ToolParameterDefaultsUtils.mergeParameters(defaults, Map.of())).isEqualTo(defaults);
    }

    @Test
    void testMergeParametersReplacesNonObjectDefaultsWithObjectValues() {
        Map<String, Object> mergedParameters = ToolParameterDefaultsUtils.mergeParameters(
            Map.of("body", "raw"), Map.of("body", Map.of("content", "x")));

        assertThat(mergedParameters).isEqualTo(Map.of("body", Map.of("content", "x")));
    }

    @Test
    void testRemoveClearedDefaultsDropsDefaultsOfExplicitlyClearedValues() {
        Map<String, Object> savedBody = new HashMap<>();

        savedBody.put("content", "=fromAi('content', 'STRING')");
        savedBody.put("format", null);

        Map<String, Object> savedParameters = new HashMap<>();

        savedParameters.put("length", null);
        savedParameters.put("body", savedBody);

        Map<String, Object> remainingDefaults = ToolParameterDefaultsUtils.removeClearedDefaults(
            Map.of("length", 8L, "flag", false, "body", Map.of("format", "text", "limit", 10L)), savedParameters);

        assertThat(remainingDefaults).isEqualTo(Map.of("flag", false, "body", Map.of("limit", 10L)));
    }

    @Test
    void testRemoveClearedDefaultsKeepsDefaultsOfUnresolvedAiValues() {
        Map<String, Object> mergedParameters = ToolParameterDefaultsUtils.mergeParameters(
            ToolParameterDefaultsUtils.removeClearedDefaults(
                Map.of("format", "text"), Map.of("format", "=fromAi('format', 'STRING')")),
            new HashMap<>(Collections.singletonMap("format", null)));

        assertThat(mergedParameters).isEqualTo(Map.of("format", "text"));
    }
}
