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

package com.bytechef.component.notion.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.NAME;
import static com.bytechef.component.notion.constant.NotionConstants.SUPPORTED_PROPERTY_TYPES;
import static com.bytechef.component.notion.constant.NotionConstants.TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TITLE;
import static com.bytechef.component.notion.constant.NotionConstants.TYPE;
import static com.bytechef.component.notion.util.NotionPropertyType.CHECKBOX;
import static com.bytechef.component.notion.util.NotionPropertyType.DATE;
import static com.bytechef.component.notion.util.NotionPropertyType.EMAIL;
import static com.bytechef.component.notion.util.NotionPropertyType.MULTI_SELECT;
import static com.bytechef.component.notion.util.NotionPropertyType.NUMBER;
import static com.bytechef.component.notion.util.NotionPropertyType.PHONE_NUMBER;
import static com.bytechef.component.notion.util.NotionPropertyType.RICH_TEXT;
import static com.bytechef.component.notion.util.NotionPropertyType.SELECT;
import static com.bytechef.component.notion.util.NotionPropertyType.STATUS;
import static com.bytechef.component.notion.util.NotionPropertyType.URL;
import static com.bytechef.component.notion.util.NotionPropertyType.getPropertyTypeByName;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionUtils {

    private NotionUtils() {
    }

    public static Map<String, Object> convertPropertiesToNotionValues(
        Context context, Map<String, ?> fields, String databaseId) {

        Map<String, Object> propertiesMap = new HashMap<>();

        Map<String, String> propertyNameTypeMap = createPropertyNameTypeMap(databaseId, context);

        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            String name = propertyNameTypeMap.get(fieldName);
            NotionPropertyType enumType = getPropertyTypeByName(name);

            if (SUPPORTED_PROPERTY_TYPES.contains(enumType)) {
                switch (enumType) {
                    case CHECKBOX -> propertiesMap.put(fieldName, Map.of(CHECKBOX.getName(), fieldValue));
                    case DATE -> propertiesMap.put(fieldName, Map.of(DATE.getName(), Map.of("start", fieldValue)));
                    case EMAIL -> propertiesMap.put(fieldName, Map.of(EMAIL.getName(), fieldValue));
                    case SELECT -> propertiesMap.put(fieldName, Map.of(SELECT.getName(), Map.of(NAME, fieldValue)));
                    case MULTI_SELECT -> {
                        if (fieldValue instanceof List<?> list) {
                            List<Map<String, String>> multiSelectValues = list.stream()
                                .map(item -> Map.of(NAME, item.toString()))
                                .toList();

                            propertiesMap.put(fieldName, Map.of(MULTI_SELECT.getName(), multiSelectValues));
                        }
                    }
                    case STATUS -> propertiesMap.put(fieldName, Map.of(STATUS.getName(), Map.of(NAME, fieldValue)));
                    case NUMBER -> propertiesMap.put(fieldName, Map.of(NUMBER.getName(), fieldValue));
                    case PHONE_NUMBER -> propertiesMap.put(fieldName, Map.of(PHONE_NUMBER.getName(), fieldValue));
                    case RICH_TEXT -> propertiesMap.put(fieldName, Map.of(
                        RICH_TEXT.getName(), List.of(
                            Map.of(
                                TYPE, TEXT,
                                TEXT, Map.of(CONTENT, fieldValue)))));
                    case TITLE -> propertiesMap.put(fieldName, Map.of(
                        NotionPropertyType.TITLE.getName(), List.of(
                            Map.of(
                                TYPE, TEXT,
                                TEXT, Map.of(CONTENT, fieldValue)))));
                    case URL -> propertiesMap.put(fieldName, Map.of(URL.getName(), fieldValue));
                    default -> context.log(log -> log.info("Property with type '{}' is not supported yet.", name));
                }
            }
        }

        return propertiesMap;
    }

    public static List<ModifiableValueProperty<?, ?>> createPropertiesForDatabaseItem(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        Context context) {

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();
        String databaseId = inputParameters.getRequiredString(ID);

        Map<String, ?> body = getDatabase(databaseId, context);

        if (body.get("properties") instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String propertyName = (String) entry.getKey();

                Object value = entry.getValue();

                if (value instanceof Map<?, ?> propertyMap) {
                    String type = (String) propertyMap.get(TYPE);

                    NotionPropertyType enumType = getPropertyTypeByName(type);

                    if (SUPPORTED_PROPERTY_TYPES.contains(enumType)) {
                        ModifiableValueProperty<?, ?> property = createPropertyForType(
                            enumType, propertyName, propertyMap);

                        if (property != null) {
                            properties.add(property);
                        }
                    }
                }
            }
        }

        return properties;
    }

    public static List<Object> getAllItems(
        Context context, String url, boolean editorEnvironment, Object... additionalBodyParameters) {

        List<Object> items = new ArrayList<>();

        String startCursor = null;
        int pageSize = editorEnvironment ? 1 : 100;

        do {
            List<Object> bodyParameters = new ArrayList<>();

            bodyParameters.add("page_size");
            bodyParameters.add(pageSize);
            bodyParameters.add("start_cursor");
            bodyParameters.add(startCursor);

            bodyParameters.addAll(List.of(additionalBodyParameters));

            Map<String, ?> body = context
                .http(http -> http.post(url))
                .body(Http.Body.of(bodyParameters.toArray()))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("results") instanceof List<?> list) {
                items.addAll(list);
            }

            if (editorEnvironment) {
                break;
            }

            startCursor = (String) body.get("next_cursor");
        } while (startCursor != null);

        return items;
    }

    public static Map<String, ?> getDatabase(String databaseId, Context context) {
        return context.http(http -> http.get("/databases/%s".formatted(databaseId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static List<Option<String>> getDatabaseIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        List<Object> items = getAllItems(
            context, "/search", false, "filter", Map.of("property", "object", "value", "database"));

        for (Object object : items) {
            if (object instanceof Map<?, ?> map) {
                options.add(getOption((String) map.get(ID), map));
            }
        }

        return options;
    }

    public static List<Option<String>> getPageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        List<Object> items = getAllItems(
            context, "/search", false, "filter", Map.of("property", "object", "value", "page"));

        for (Object item : items) {
            if (item instanceof Map<?, ?> resultMap &&
                resultMap.get("properties") instanceof Map<?, ?> properties &&
                properties.get(TITLE) instanceof Map<?, ?> title) {

                options.add(getOption((String) resultMap.get(ID), title));
            }
        }

        return options;
    }

    private static Option<String> getOption(String id, Map<?, ?> titleMap) {
        if (titleMap.get(TITLE) instanceof List<?> list) {
            Object titleItem = list.getFirst();

            if (titleItem instanceof Map<?, ?> title && title.get(TEXT) instanceof Map<?, ?> text) {
                return option((String) text.get(CONTENT), id);
            }
        }

        return null;
    }

    private static Map<String, String> createPropertyNameTypeMap(String databaseId, Context context) {
        Map<String, ?> body = getDatabase(databaseId, context);

        Map<String, String> columnIdTypeMap = new HashMap<>();

        if (body.get("properties") instanceof Map<?, ?> propertyMap) {
            for (Map.Entry<?, ?> property : propertyMap.entrySet()) {
                String propertyName = (String) property.getKey();

                Object value = property.getValue();

                if (value instanceof Map<?, ?> map) {
                    columnIdTypeMap.put(propertyName, (String) map.get(TYPE));
                }
            }
        }

        return columnIdTypeMap;
    }

    private static ModifiableValueProperty<?, ?> createPropertyForType(
        NotionPropertyType enumType, String propertyName, Map<?, ?> propertyDetails) {

        String description = (String) propertyDetails.get("description");

        return switch (enumType) {
            case CHECKBOX -> bool(propertyName)
                .label(propertyName)
                .description(description)
                .required(false);
            case DATE -> dateTime(propertyName)
                .label(propertyName)
                .description(description)
                .required(false);
            case EMAIL, PHONE_NUMBER, TITLE, URL -> string(propertyName)
                .label(propertyName)
                .description(description)
                .required(false);
            case SELECT -> string(propertyName)
                .label(propertyName)
                .description(description)
                .options(extractOptions(propertyDetails, SELECT.getName()))
                .required(false);
            case MULTI_SELECT -> array(propertyName)
                .label(propertyName)
                .description(description)
                .items(string())
                .options(extractOptions(propertyDetails, MULTI_SELECT.getName()))
                .required(false);
            case STATUS -> string(propertyName)
                .label(propertyName)
                .description(description)
                .options(extractOptions(propertyDetails, STATUS.getName()))
                .required(false);
            case NUMBER -> number(propertyName)
                .label(propertyName)
                .description(description)
                .required(false);
            case RICH_TEXT -> string(propertyName)
                .label(propertyName)
                .description(description)
                .controlType(ControlType.TEXT_AREA)
                .required(false);
            default -> null;
        };
    }

    private static List<Option<String>> extractOptions(Map<?, ?> propertyDetails, String key) {
        List<Option<String>> options = new ArrayList<>();

        if (propertyDetails.get(key) instanceof Map<?, ?> selectMap &&
            selectMap.get("options") instanceof List<?> optionsList) {

            for (Object optionObj : optionsList) {
                if (optionObj instanceof Map<?, ?> optionMap) {
                    String name = (String) optionMap.get(NAME);
                    String desc = (String) optionMap.get("description");

                    options.add(option(name, name, desc));
                }
            }
        }

        return options;
    }
}
