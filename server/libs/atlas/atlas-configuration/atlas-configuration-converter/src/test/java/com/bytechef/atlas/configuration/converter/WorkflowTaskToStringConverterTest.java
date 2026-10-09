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

package com.bytechef.atlas.configuration.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTaskToStringConverterTest {

    private static final Map<String, Object> CLUSTER_ELEMENTS = Map.of(
        "model", Map.of("name", "anthropic_1", "type", "anthropic/v1/model", "parameters", Map.of("model", "m")));

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .build();

    @Test
    void testConvertKeepsTheTaskExtensionsThroughARoundTrip() {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of(
                "clusterElements", CLUSTER_ELEMENTS, "name", "aiAgent_1", "parameters", Map.of("userPrompt", "Hi"),
                "type", "aiAgent/v1/streamChat"));

        String json = new WorkflowTaskToStringConverter(objectMapper).convert(workflowTask);

        WorkflowTask readWorkflowTask = new StringToWorkflowTaskConverter(objectMapper).convert(json);

        Map<String, ?> extensions = readWorkflowTask.getExtensions();

        assertThat(extensions.get("clusterElements")).isEqualTo(CLUSTER_ELEMENTS);
        assertThat(readWorkflowTask.getName()).isEqualTo("aiAgent_1");
        assertThat(readWorkflowTask.getType()).isEqualTo("aiAgent/v1/streamChat");
    }
}
