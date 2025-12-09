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

package com.bytechef.component.aitable.util;

import static com.bytechef.component.aitable.constant.AITableConstants.DATA;
import static com.bytechef.component.aitable.constant.AITableConstants.DATASHEET_ID;
import static com.bytechef.component.aitable.constant.AITableConstants.FIELDS;
import static com.bytechef.component.aitable.constant.AITableConstants.ID;
import static com.bytechef.component.aitable.constant.AITableConstants.NAME;
import static com.bytechef.component.aitable.constant.AITableConstants.NOT_SUPPORTED_FIELD_TYPES;
import static com.bytechef.component.aitable.constant.AITableConstants.SPACE_ID;
import static com.bytechef.component.aitable.constant.AITableConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.aitable.constant.FieldType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class AITableUtils {

    private AITableUtils() {
    }

    public static List<? extends ValueProperty<?>> createPropertiesForRecord(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) {

        List<FieldTypeInfo> datasheetFields = createDatasheetFields(inputParameters, context);

        List<ValueProperty<?>> list = new ArrayList<>();

        for (FieldTypeInfo fieldTypeInfo : datasheetFields) {
            ModifiableValueProperty<?, ?> propertyType = getPropertyType(fieldTypeInfo);

            if (propertyType != null) {
                list.add(propertyType);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<FieldTypeInfo> createDatasheetFields(Parameters inputParameters, ActionContext context) {
        String datasheetId = inputParameters.getRequiredString(DATASHEET_ID);

        Map<String, Object> body = getDatasheetFields(context, datasheetId);

        List<FieldTypeInfo> fields = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && (map.get("fields") instanceof List<?> list)) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> field) {
                    Map<String, Object> property = null;

                    if (field.get("property") instanceof Map<?, ?> propertyMap) {
                        property = (Map<String, Object>) propertyMap;
                    }

                    fields.add(new FieldTypeInfo((String) field.get(NAME), (String) field.get(TYPE), property));
                }

            }
        }

        return fields;
    }

    private static ModifiableValueProperty<?, ?> getPropertyType(FieldTypeInfo fieldTypeInfo) {
        String type = fieldTypeInfo.type();
        String name = fieldTypeInfo.name();

        FieldType fieldType = FieldType.fromString(type);

        if (!NOT_SUPPORTED_FIELD_TYPES.contains(fieldType)) {
            return switch (fieldType) {
                case CHECKBOX -> bool(name)
                    .label(name)
                    .required(false);
                case SINGLE_TEXT, EMAIL, URL -> string(name)
                    .label(name)
                    .required(false);
                case PHONE -> string(name)
                    .label(name)
                    .controlType(ControlType.PHONE)
                    .required(false);
                case CURRENCY -> {
                    Map<String, Object> property = fieldTypeInfo.property();

                    yield number(name)
                        .label(name)
                        .description("Currency symbol: " + property.get("symbol"))
                        .required(false);
                }
                case NUMBER, PERCENT -> number(name)
                    .label(name)
                    .required(false);
                case RATING -> {
                    Map<String, Object> property = fieldTypeInfo.property();

                    yield integer(name)
                        .label(name)
                        .maxValue((Integer) property.get("max"))
                        .required(false);
                }
                case DATE_TIME -> {
                    Map<String, Object> property = fieldTypeInfo.property();

                    boolean includeTime = (Boolean) property.get("includeTime");

                    if (includeTime) {
                        yield dateTime(name)
                            .label(name)
                            .required(false);
                    } else {
                        yield date(name)
                            .label(name)
                            .required(false);
                    }
                }
                case TEXT -> string(name)
                    .label(name)
                    .controlType(ControlType.TEXT_AREA)
                    .required(false);
                case SINGLE_SELECT -> string(name)
                    .label(name)
                    .options(getOptions(fieldTypeInfo.property(), NAME))
                    .required(false);
                case MULTI_SELECT -> array(name)
                    .label(name)
                    .items(string())
                    .options(getOptions(fieldTypeInfo.property(), NAME))
                    .required(false);

                case MEMBER -> array(name)
                    .label(name)
                    .options(getOptions(fieldTypeInfo.property(), ID))
                    .items(string())
                    .required(false);

                case TWO_WAY_LINK -> array(name)
                    .label(name)
                    .required(false);
                default -> null;
            };
        }

        return null;
    }

    private static List<Option<String>> getOptions(Map<String, Object> property, String optionValue) {
        List<Option<String>> options = new ArrayList<>();

        Object o = property.get("options");

        if (o instanceof List<?> list) {
            for (Object option : list) {
                if (option instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(optionValue)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getDatasheetIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext context) {

        String spaceId = inputParameters.getRequiredString(SPACE_ID);

        Map<String, Object> body = context.http(http -> http.get("/spaces/" + spaceId + "/nodes"))
            .queryParameters(TYPE, "Datasheet")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && (map.get("nodes") instanceof List<?> list)) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> field) {
                    options.add(option((String) field.get(NAME), (String) field.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getFieldNamesOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        String datasheetId = inputParameters.getRequiredString(DATASHEET_ID);

        Map<String, Object> body = getDatasheetFields(context, datasheetId);

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && (map.get(FIELDS) instanceof List<?> list)) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> field) {
                    options.add(option((String) field.get(NAME), (String) field.get(NAME)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getSpaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context.http(http -> http.get("/spaces"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && (map.get("spaces") instanceof List<?> list)) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> field) {
                    options.add(option((String) field.get(NAME), (String) field.get(ID)));
                }
            }
        }

        return options;
    }

    private static Map<String, Object> getDatasheetFields(ActionContext context, String datasheetId) {
        return context.http(http -> http.get("/datasheets/" + datasheetId + "/fields"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private record FieldTypeInfo(String name, String type, Map<String, Object> property) {
    }
}
