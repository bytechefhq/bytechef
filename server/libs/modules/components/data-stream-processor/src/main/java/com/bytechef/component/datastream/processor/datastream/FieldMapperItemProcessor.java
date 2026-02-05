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

package com.bytechef.component.datastream.processor.datastream;

import static com.bytechef.component.datastream.processor.constant.DataStreamProcessorConstants.DEFAULT_VALUE;
import static com.bytechef.component.datastream.processor.constant.DataStreamProcessorConstants.DESTINATION_FIELD;
import static com.bytechef.component.datastream.processor.constant.DataStreamProcessorConstants.MAPPINGS;
import static com.bytechef.component.datastream.processor.constant.DataStreamProcessorConstants.SOURCE_FIELD;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;
import static com.bytechef.component.definition.datastream.ItemReader.SOURCE;
import static com.bytechef.component.definition.datastream.ItemWriter.DESTINATION;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ClusterElementDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.component.definition.datastream.FieldMapping;
import com.bytechef.component.definition.datastream.FieldsProvider;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.datastream.ItemProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Mapping processor that transforms data between source and destination schemas. Only mapped fields pass through;
 * unmapped source fields are dropped. Missing source fields use the configured default value or null.
 *
 * @author Ivica Cardic
 */
public class FieldMapperItemProcessor implements ItemProcessor<Object, Object> {

    public static final String DESTINATION_SCHEMA = "destinationSchema";
    public static final String SCHEMAS = "schemas";
    public static final String SOURCE_SCHEMA = "sourceSchema";
    public static final String USE_JSON_SCHEMA = "useJsonSchema";

    public static final ModifiableClusterElementDefinition<FieldMapperItemProcessor> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<FieldMapperItemProcessor>clusterElement("fieldMapper")
            .title("Field Mapper")
            .description("Maps fields from source to destination columns.")
            .type(PROCESSOR)
            .object(FieldMapperItemProcessor::new)
            .properties(
                bool(USE_JSON_SCHEMA)
                    .label("Use JSON Schema")
                    .description("Enable to manually define field schemas using JSON Schema format.")
                    .expressionEnabled(false)
                    .defaultValue(false),
                dynamicProperties(SCHEMAS)
                    .header("Field Schemas")
                    .propertiesLookupDependsOn(USE_JSON_SCHEMA)
                    .properties(FieldMapperItemProcessor::getSchemaProperties),
                array(MAPPINGS)
                    .label("Field Mappings")
                    .description("List of field mappings from source to destination.")
                    .required(true)
                    .items(
                        object()
                            .properties(
                                string(SOURCE_FIELD)
                                    .label("Source Field")
                                    .description("The field name from the source.")
                                    .options(
                                        (OptionsFunction<String>) FieldMapperItemProcessor::getSourceFieldOptions)
                                    .required(true),
                                string(DESTINATION_FIELD)
                                    .label("Destination Field")
                                    .description("The field name for the destination.")
                                    .options(
                                        (OptionsFunction<String>) FieldMapperItemProcessor::getDestinationFieldOptions)
                                    .required(true),
                                string(DEFAULT_VALUE)
                                    .label("Default Value")
                                    .description("Default value when source field is missing."))));

    public static List<? extends Option<String>> getDestinationFieldOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ClusterElementContext context) {

        return getOptions(inputParameters, DESTINATION, DESTINATION_SCHEMA, context);
    }

    public static List<? extends ValueProperty<?>> getSchemaProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ClusterElementContext context) {

        if (!inputParameters.getBoolean(USE_JSON_SCHEMA, false)) {
            return List.of();
        }

        List<ValueProperty<?>> properties = new ArrayList<>();

        List<FieldDefinition> sourceFields = ((ClusterElementContextAware) context).resolveClusterElement(
            SOURCE,
            (clusterElement, inputParameters1, connectionParameters1, context1) -> ((FieldsProvider) clusterElement)
                .getFields(inputParameters1, connectionParameters1, context1));

        if (sourceFields == null || sourceFields.isEmpty()) {
            properties.add(
                string(SOURCE_SCHEMA)
                    .label("Source Schema")
                    .description("Define the source field structure when not auto-detected.")
                    .controlType(JSON_SCHEMA_BUILDER)
                    .required(true));
        }

        List<FieldDefinition> destinationFields = ((ClusterElementContextAware) context).resolveClusterElement(
            DESTINATION,
            (clusterElement, inputParameters1, connectionParameters1, context1) -> ((FieldsProvider) clusterElement)
                .getFields(inputParameters1, connectionParameters1, context1));

        if (destinationFields == null || destinationFields.isEmpty()) {
            properties.add(
                string(DESTINATION_SCHEMA)
                    .label("Destination Schema")
                    .description("Define the destination field structure when not auto-detected.")
                    .controlType(JSON_SCHEMA_BUILDER)
                    .required(true));
        }

        return properties;
    }

    public static List<? extends Option<String>> getSourceFieldOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ClusterElementContext context) {

        return getOptions(inputParameters, SOURCE, SOURCE_SCHEMA, context);
    }

    @Override
    public Map<String, Object> process(
        Map<String, Object> item, Parameters inputParameters, Parameters connectionParameters,
        ClusterElementContext context) {

        Map<String, Object> result = new HashMap<>();

        List<FieldMapping> fieldMappings = inputParameters
            .getList(MAPPINGS, new TypeReference<Map<String, Object>>() {}, List.of())
            .stream()
            .map(map -> new FieldMapping(
                (String) map.get(SOURCE_FIELD), (String) map.get(DESTINATION_FIELD), map.get(DEFAULT_VALUE)))
            .toList();

        for (FieldMapping fieldMapping : fieldMappings) {
            Object value;
            String sourceField = fieldMapping.sourceField();
            String destinationField = fieldMapping.destinationField();

            if (sourceField.contains(".")) {
                if (context.nested(nested -> nested.containsPath(item, sourceField))) {
                    value = context.nested(nested -> nested.getValue(item, sourceField));
                } else {
                    value = fieldMapping.defaultValue();
                }
            } else {
                if (item.containsKey(sourceField)) {
                    value = item.get(sourceField);
                } else {
                    value = fieldMapping.defaultValue();
                }
            }

            if (destinationField.contains(".")) {
                context.nested(nested -> nested.setValue(result, destinationField, value));
            } else {
                result.put(destinationField, value);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static void extractFieldsRecursively(
        Map<String, Object> properties, String prefix, List<FieldDefinition> fields) {

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = entry.getKey();

            if (!(entry.getValue() instanceof Map)) {
                continue;
            }

            Map<String, Object> fieldSchema = (Map<String, Object>) entry.getValue();
            String fullPath = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
            String type = (String) fieldSchema.get("type");
            Object nestedPropertiesObj = fieldSchema.get("properties");

            if ("object".equals(type) && nestedPropertiesObj instanceof Map) {
                extractFieldsRecursively((Map<String, Object>) nestedPropertiesObj, fullPath, fields);
            } else {
                String label = fieldSchema.containsKey("title")
                    ? (String) fieldSchema.get("title")
                    : fullPath;

                fields.add(new FieldDefinition(fullPath, label));
            }
        }
    }

    @NonNull
    private static List<? extends Option<String>> getOptions(
        Parameters inputParameters, ClusterElementType clusterElementType, String jsonSchema,
        ClusterElementContext context) {

        List<FieldDefinition> fields = ((ClusterElementContextAware) context).resolveClusterElement(
            clusterElementType,
            (clusterElement, inputParameters1, connectionParameters1, context1) -> ((FieldsProvider) clusterElement)
                .getFields(inputParameters1, connectionParameters1, context1));

        if (fields != null && !fields.isEmpty()) {
            return fields.stream()
                .map(field -> option(field.label(), field.name()))
                .toList();
        }

        String schemaJson = inputParameters.getString(jsonSchema);

        if (schemaJson != null && !schemaJson.isBlank()) {
            return parseFieldsFromJsonSchema(schemaJson, context).stream()
                .map(field -> option(field.label(), field.name()))
                .toList();
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<FieldDefinition> parseFieldsFromJsonSchema(String schemaJson, ClusterElementContext context) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return List.of();
        }

        try {
            Map<String, ?> schema = context.json(json -> json.readMap(schemaJson));
            Object propertiesObj = schema.get("properties");

            if (!(propertiesObj instanceof Map)) {
                return List.of();
            }

            List<FieldDefinition> fields = new ArrayList<>();

            extractFieldsRecursively((Map<String, Object>) propertiesObj, "", fields);

            return fields;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
