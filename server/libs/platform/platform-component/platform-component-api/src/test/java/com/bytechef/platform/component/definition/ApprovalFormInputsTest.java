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

package com.bytechef.platform.component.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.definition.ApprovalFormInputs.Resolution;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalFormInputsTest {

    private static final List<Map<String, Object>> SELECT_OPTIONS = List.of(
        Map.of("label", "Red", "value", "red"), Map.of("label", "Blue", "value", "blue"));

    @Test
    void testResolveWithoutInputsKeepsTheData() {
        Resolution resolution = ApprovalFormInputs.resolve(null, Map.of("approved", true), true);

        assertThat(resolution.isValid()).isTrue();
        assertThat(resolution.data()).isEqualTo(Map.of("approved", true));
    }

    @Test
    void testResolveReportsEveryMissingRequiredInputOfAnApproval() {
        List<Map<String, Object>> inputs = List.of(
            Map.of("fieldName", "reason", "fieldLabel", "Reason", "fieldType", 6, "required", true),
            Map.of("fieldName", "tags", "fieldType", 7, "multipleChoice", true, "required", true),
            Map.of("fieldName", "agree", "fieldType", 1, "required", true),
            Map.of("fieldType", 12, "defaultValue", "<p>Read me</p>", "required", true));

        Resolution resolution = ApprovalFormInputs.resolve(
            inputs, Map.of("approved", true, "reason", "", "tags", List.of(), "agree", false), true);

        assertThat(resolution.errors()).containsExactly(
            "'Reason' is required", "'tags' is required", "'agree' is required");
    }

    @Test
    void testResolveLetsADiscardOmitRequiredInputs() {
        List<Map<String, Object>> inputs = List.of(
            Map.of("fieldName", "reason", "fieldType", 6, "required", true),
            Map.of("fieldName", "amount", "fieldType", 9, "required", true));

        Resolution resolution = ApprovalFormInputs.resolve(inputs, Map.of("approved", false, "amount", "x"), false);

        assertThat(resolution.isValid()).isTrue();
        assertThat(resolution.data())
            .containsEntry("reason", "")
            .containsEntry("amount", "x");
    }

    @Test
    void testResolveCoercesValuesToTheirDeclaredTypes() {
        List<Map<String, Object>> inputs = List.of(
            Map.of("fieldName", "count", "fieldType", 9),
            Map.of("fieldName", "price", "fieldType", 9),
            Map.of("fieldName", "big", "fieldType", 9),
            Map.of("fieldName", "native", "fieldType", 9),
            Map.of("fieldName", "agree", "fieldType", 1),
            Map.of("fieldName", "title", "fieldType", 6),
            Map.of("fieldName", "color", "fieldType", 7, "fieldOptions", SELECT_OPTIONS),
            Map.of("fieldName", "colors", "fieldType", 7, "multipleChoice", true, "fieldOptions", SELECT_OPTIONS),
            Map.of("fieldName", "day", "fieldType", 2),
            Map.of("fieldName", "moment", "fieldType", 3));

        Map<String, Object> data = Map.of(
            "count", "7", "price", " 7.50 ", "big", "1e3", "native", 42, "agree", "true", "title", 12, "color", "red",
            "colors", "blue", "day", "2026-10-17", "moment", "2026-10-17T10:15:30.000Z");

        Resolution resolution = ApprovalFormInputs.resolve(inputs, data, true);

        assertThat(resolution.isValid()).isTrue();
        assertThat(resolution.data())
            .containsEntry("count", 7L)
            .containsEntry("price", 7.5)
            .containsEntry("big", 1000L)
            .containsEntry("native", 42)
            .containsEntry("agree", true)
            .containsEntry("title", "12")
            .containsEntry("color", "red")
            .containsEntry("colors", List.of("blue"))
            .containsEntry("day", "2026-10-17")
            .containsEntry("moment", "2026-10-17T10:15:30.000Z");
    }

    @Test
    void testResolveRejectsValuesThatDoNotMatchTheirDeclaredTypes() {
        List<Map<String, Object>> inputs = List.of(
            Map.of("fieldName", "count", "fieldType", 9),
            Map.of("fieldName", "agree", "fieldType", 1),
            Map.of("fieldName", "email", "fieldType", 8),
            Map.of("fieldName", "day", "fieldType", 2),
            Map.of("fieldName", "color", "fieldType", 11, "fieldOptions", SELECT_OPTIONS),
            Map.of(
                "fieldName", "colors", "fieldType", 7, "multipleChoice", true, "fieldOptions", SELECT_OPTIONS,
                "maxSelection", 1),
            Map.of("fieldName", "title", "fieldType", 6));

        Map<String, Object> data = Map.of(
            "count", "seven", "agree", "maybe", "email", "nobody", "day", "17/10/2026", "color", "green", "colors",
            List.of("red", "blue"), "title", Map.of("nested", true));

        Resolution resolution = ApprovalFormInputs.resolve(inputs, data, true);

        assertThat(resolution.errors()).containsExactly(
            "'count' must be a number", "'agree' must be true or false", "'email' must be an email address",
            "'day' must be an ISO-8601 date or date-time", "'color' must be one of red, blue",
            "'colors' allows at most 1 selections", "'title' must be a text value");
    }

    @Test
    void testResolveAddsEveryAbsentInputWithAnEmptyValue() {
        List<Map<String, Object>> inputs = List.of(
            Map.of("fieldName", "title", "fieldType", 6),
            Map.of("fieldName", "notes", "fieldType", 5),
            Map.of("fieldName", "count", "fieldType", 9),
            Map.of("fieldName", "agree", "fieldType", 1),
            Map.of("fieldName", "day", "fieldType", 2),
            Map.of("fieldName", "color", "fieldType", 7),
            Map.of("fieldName", "colors", "fieldType", 7, "multipleChoice", true),
            Map.of("fieldName", "source", "fieldType", 13, "defaultValue", "crm"),
            Map.of("fieldName", "attachment", "fieldType", 4));

        Resolution resolution = ApprovalFormInputs.resolve(inputs, Map.of("approved", true), true);

        assertThat(resolution.isValid()).isTrue();
        assertThat(resolution.data())
            .containsEntry("title", "")
            .containsEntry("notes", "")
            .containsEntry("count", null)
            .containsEntry("agree", false)
            .containsEntry("day", null)
            .containsEntry("color", "")
            .containsEntry("colors", List.of())
            .containsEntry("source", "crm")
            .containsEntry("attachment", null);
    }
}
