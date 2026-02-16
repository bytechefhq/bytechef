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

package com.bytechef.component.microsoft.dynamics.crm.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.ENTITY_NAME;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.LOOKUP;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.MEMO;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.MONEY;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.OWNER;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.UNIQUE_IDENTIFIER;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.VIRTUAL;
import static com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType.getEntityAttributeType;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.ENTITY_TYPE;

import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.dynamics.crm.constant.EntityAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmUtils {

    private static final List<EntityAttributeType> UNSUPPORTED_ENTITY_TYPES = List.of(
        ENTITY_NAME, LOOKUP, MEMO, MONEY, OWNER, VIRTUAL, UNIQUE_IDENTIFIER);

    private MicrosoftDynamicsCrmUtils() {
    }

    public static PropertiesFunction getEntityFieldsProperties(boolean isNewRecord) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, context) -> {

            List<ValueProperty<?>> properties = new ArrayList<>();

            Map<String, Object> entityTypeAttributes = getEntityTypeAttributes(
                inputParameters.getRequiredString(ENTITY_TYPE), context);

            if (entityTypeAttributes.get("value") instanceof List<?> list && !list.isEmpty() &&
                list.getFirst() instanceof Map<?, ?> map) {

                String logicalName = (String) map.get("LogicalName");

                List<Map<String, Object>> attributes = getAttributes(logicalName, context);

                for (Map<String, Object> attribute : attributes) {
                    String attributeType = (String) attribute.get("AttributeType");

                    EntityAttributeType entityAttributeType = getEntityAttributeType(attributeType);

                    if (entityAttributeType != null) {
                        if ((Boolean) attribute.get("IsValidForCreate") &&
                            !UNSUPPORTED_ENTITY_TYPES.contains(entityAttributeType)) {

                            properties.add(
                                createProperty(attribute, entityAttributeType, logicalName, isNewRecord, context));
                        }
                    } else {
                        context.log(log -> log.info("Attribute type '{}' is not supported yet.", attributeType));
                    }
                }
            }

            return properties
                .stream()
                .filter(Objects::nonNull)
                .toList();
        };
    }

    private static Map<String, Object> getEntityTypeAttributes(String entityType, Context context) {
        return context.http(http -> http.get("/EntityDefinitions"))
            .queryParameters(
                "$select", "PrimaryIdAttribute,PrimaryNameAttribute,LogicalName",
                "$filter", "EntitySetName eq '%s'".formatted(entityType))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getAttributes(String logicalName, Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/EntityDefinitions(LogicalName='%s')/Attributes".formatted(logicalName)))
            .queryParameters(
                "$select", "AttributeType,LogicalName,Description,DisplayName,IsPrimaryName,IsValidForCreate")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("value") instanceof List<?> list) {
            return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
        }

        return List.of();
    }

    private static ValueProperty<?> createProperty(
        Map<String, Object> attribute, EntityAttributeType entityAttributeType, String logicalName,
        boolean isNewRecord, Context context) {

        String name = (String) attribute.get("LogicalName");
        String displayName = name;

        if (attribute.get("DisplayName") instanceof Map<?, ?>) {
            displayName = getLabel(attribute.get("DisplayName"));
        }

        String description = getLabel(attribute.get("Description"));
        boolean required = (Boolean) attribute.get("IsPrimaryName") && isNewRecord;

        return switch (entityAttributeType) {
            case BIGINT, DECIMAL, DOUBLE -> number(name)
                .label(displayName)
                .description(description)
                .required(required);
            case INTEGER -> integer(name)
                .label(displayName)
                .description(description)
                .required(required);
            case DATETIME -> dateTime(name)
                .label(displayName)
                .description(description)
                .required(required);
            case BOOLEAN -> bool(name)
                .label(displayName)
                .description(description)
                .required(required);
            case STRING -> string(name)
                .label(displayName)
                .description(description)
                .required(required);
            case PICKLIST, STATUS, STATE -> string(name)
                .label(displayName)
                .description(description)
                .options(
                    getOptionsFieldValues(
                        context, logicalName, name, (String) attribute.get("AttributeType")))
                .required(required);
            default -> null;
        };
    }

    private static String getLabel(Object object) {
        String label = "";

        if (object instanceof Map<?, ?> map && map.get("UserLocalizedLabel") instanceof Map<?, ?> userLocalizedLabel) {
            label = (String) userLocalizedLabel.get("Label");
        }

        return label;
    }

    private static List<Option<String>> getOptionsFieldValues(
        Context context, String logicalName, String name, String attributeType) {

        Map<String, ?> body = context
            .http(http -> http.get(
                "/EntityDefinitions(LogicalName='%s')/Attributes(LogicalName='%s')/Microsoft.Dynamics.CRM.%sAttributeMetadata"
                    .formatted(logicalName, name, attributeType)))
            .queryParameters(
                "$select", "LogicalName",
                "$expand", "OptionSet($select=Options),GlobalOptionSet($select=Options)")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("OptionSet") instanceof Map<?, ?> optionSet && optionSet.get("Options") instanceof List<?> list) {
            return getOptions(list);
        }

        if (body.get("GlobalOptionSet") instanceof Map<?, ?> globalOptionSet &&
            globalOptionSet.get("Options") instanceof List<?> list) {

            return getOptions(list);
        }

        return List.of();
    }

    private static List<Option<String>> getOptions(List<?> list) {
        List<Option<String>> options = new ArrayList<>();

        for (Object o : list) {
            if (o instanceof Map<?, ?> map) {
                String label = getLabel(map.get("Label"));
                String value = String.valueOf(map.get("Value"));

                if (label.isEmpty()) {
                    label = value;
                }

                options.add(option(label, value));
            }
        }

        return options;
    }

    public static List<Option<String>> getEntityTypeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> body = context
            .http(http -> http.get("/EntityDefinitions"))
            .queryParameters("$select", "EntitySetName,LogicalName")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("value") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String entitySetName = (String) map.get("EntitySetName");

                    options.add(option(entitySetName, entitySetName));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getRecordIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        String entityType = inputParameters.getRequiredString(ENTITY_TYPE);
        Map<String, Object> entityTypeAttributes = getEntityTypeAttributes(entityType, context);

        if (entityTypeAttributes.get("value") instanceof List<?> list) {
            Object object = list.getFirst();

            if (object instanceof Map<?, ?> map) {
                String primaryIdAttribute = (String) map.get("PrimaryIdAttribute");
                String primaryNameAttribute = (String) map.get("PrimaryNameAttribute");

                Map<String, Object> body = context
                    .http(http -> http.get("/%s".formatted(entityType)))
                    .queryParameters("$select", primaryNameAttribute)
                    .configuration(Http.responseType(Http.ResponseType.JSON))
                    .execute()
                    .getBody(new TypeReference<>() {});

                if (body.get("value") instanceof List<?> list2) {
                    for (Object o : list2) {
                        if (o instanceof Map<?, ?> map2) {
                            options.add(
                                option((String) map2.get(primaryNameAttribute), (String) map2.get(primaryIdAttribute)));
                        }
                    }
                }
            }
        }

        return options;
    }
}
