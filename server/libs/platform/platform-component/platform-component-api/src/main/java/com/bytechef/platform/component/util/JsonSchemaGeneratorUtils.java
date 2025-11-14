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

package com.bytechef.platform.component.util;

import com.bytechef.platform.component.domain.Property;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.util.json.JsonParser;

/**
 * Generates JSON schema for input parameters.
 *
 * @author Ivica Cardic
 */
public class JsonSchemaGeneratorUtils {

    private static final SchemaGenerator TYPE_SCHEMA_GENERATOR;

    static {
        Module jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);

        SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder =
            new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .with(Option.PLAIN_DEFINITION_KEYS);

        SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();

        TYPE_SCHEMA_GENERATOR = new SchemaGenerator(typeSchemaGeneratorConfig);
    }

    public static String generateInputSchema(List<? extends Property> properties) {
        ObjectMapper objectMapper = JsonParser.getObjectMapper();

        ObjectNode schemaObjectNode = objectMapper.createObjectNode();

        schemaObjectNode.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
        schemaObjectNode.put("type", "object");

        ObjectNode propertiesObjectNode = schemaObjectNode.putObject("properties");
        Set<String> required = new HashSet<>();

        for (Property property : properties) {
            String parameterName = property.getName();

            if (property.getRequired()) {
                required.add(parameterName);
            }

            // TODO check array and object, it seems schema is not generated correctly
            ObjectNode parameterObjectNode = TYPE_SCHEMA_GENERATOR.generateSchema(getType(property.getType()));
            String parameterDescription = property.getDescription();

            if (StringUtils.isNotEmpty(parameterDescription)) {
                parameterObjectNode.put("description", parameterDescription);
            }

            propertiesObjectNode.set(parameterName, parameterObjectNode);
        }

        var requiredArray = schemaObjectNode.putArray("required");

        required.forEach(requiredArray::add);

        return schemaObjectNode.toPrettyString();
    }

    private static Type getType(com.bytechef.component.definition.Property.Type type) {
        return switch (type) {
            case com.bytechef.component.definition.Property.Type.ARRAY -> List.class;
            case com.bytechef.component.definition.Property.Type.BOOLEAN -> Boolean.class;
            case com.bytechef.component.definition.Property.Type.DATE -> LocalDate.class;
            case com.bytechef.component.definition.Property.Type.DATE_TIME -> LocalDateTime.class;
            case com.bytechef.component.definition.Property.Type.INTEGER -> Integer.class;
            case com.bytechef.component.definition.Property.Type.NUMBER -> Double.class;
            case com.bytechef.component.definition.Property.Type.STRING -> String.class;
            case com.bytechef.component.definition.Property.Type.DYNAMIC_PROPERTIES,
                com.bytechef.component.definition.Property.Type.FILE_ENTRY,
                com.bytechef.component.definition.Property.Type.OBJECT -> Map.class;
            case com.bytechef.component.definition.Property.Type.TIME -> LocalTime.class;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
