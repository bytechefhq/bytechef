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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ToolValidationErrorsTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testValidationFailureCarriesTheExceptionMessage() throws Exception {
        String result = ToolValidationErrors.validationFailure(
            ToolValidationErrorsTest.class, "dropDataTable",
            new IllegalArgumentException("Table name is required"));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("Table name is required");
    }

    @Test
    void testValidationFailureWithoutMessageNamesTheToolAndExceptionType() throws Exception {
        String result = ToolValidationErrors.validationFailure(
            ToolValidationErrorsTest.class, "dropDataTable", new IllegalStateException());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("dropDataTable could not complete the request (IllegalStateException)");
    }
}
