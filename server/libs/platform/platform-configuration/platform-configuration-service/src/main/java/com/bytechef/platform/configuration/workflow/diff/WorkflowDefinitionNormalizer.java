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

package com.bytechef.platform.configuration.workflow.diff;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Ivica Cardic
 */
public class WorkflowDefinitionNormalizer {

    private WorkflowDefinitionNormalizer() {
    }

    public static Map<String, Object> normalize(Workflow workflow) {
        Map<String, Object> definitionMap;

        try {
            WorkflowResource workflowResource = new WorkflowResource(
                "diff", Map.of(), new ByteArrayResource(workflow.getDefinition()
                    .getBytes(StandardCharsets.UTF_8)),
                workflow.getFormat());

            definitionMap = WorkflowReader.readWorkflowMap(workflowResource);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse workflow definition", exception);
        }

        Map<String, Object> maskedMap = SensitiveValueMasker.mask(definitionMap);

        if (maskedMap.get("tasks") instanceof List<?> tasks) {
            for (Object task : tasks) {
                if (task instanceof Map<?, ?> taskMap) {
                    taskMap.remove(WorkflowConstants.TASK_NUMBER);
                }
            }
        }

        return sort(maskedMap);
    }

    public static String toJson(Map<String, Object> normalized) {
        return JsonUtils.writeWithDefaultPrettyPrinter(plainMasks(normalized));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sort(Map<String, Object> map) {
        Map<String, Object> sortedMap = new TreeMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sortedMap.put(entry.getKey(), sortValue(entry.getValue()));
        }

        return sortedMap;
    }

    @SuppressWarnings("unchecked")
    private static Object sortValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return sort((Map<String, Object>) map);
        }

        if (value instanceof List<?> list) {
            List<Object> sortedList = new ArrayList<>();

            for (Object item : list) {
                sortedList.add(sortValue(item));
            }

            return sortedList;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private static Object plainMasks(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> plainMap = new TreeMap<>();

            for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                plainMap.put(entry.getKey(), plainMasks(entry.getValue()));
            }

            return plainMap;
        }

        if (value instanceof List<?> list) {
            List<Object> plainList = new ArrayList<>();

            for (Object item : list) {
                plainList.add(plainMasks(item));
            }

            return plainList;
        }

        if (value instanceof String string && string.startsWith(SensitiveValueMasker.MASK)) {
            return SensitiveValueMasker.MASK;
        }

        return value;
    }
}
