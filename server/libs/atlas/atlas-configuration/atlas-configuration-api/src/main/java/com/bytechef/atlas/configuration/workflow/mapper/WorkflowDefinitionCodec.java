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

package com.bytechef.atlas.configuration.workflow.mapper;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.commons.util.JsonUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author Ivica Cardic
 */
public final class WorkflowDefinitionCodec {

    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    private WorkflowDefinitionCodec() {
    }

    public static Map<String, Object> read(String definition, Format format) {
        Map<String, Object> map = switch (format) {
            case JSON -> JsonUtils.read(definition, new TypeReference<LinkedHashMap<String, Object>>() {});
            case YAML -> YAML_OBJECT_MAPPER.readValue(
                definition, new TypeReference<LinkedHashMap<String, Object>>() {});
        };

        return map == null ? new LinkedHashMap<>() : map;
    }

    public static String write(Map<String, Object> definition, Format format) {
        return switch (format) {
            case JSON -> JsonUtils.writeWithDefaultPrettyPrinter(definition);
            case YAML -> YAML_OBJECT_MAPPER.writeValueAsString(definition);
        };
    }
}
