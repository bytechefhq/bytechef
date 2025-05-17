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

package com.bytechef.component.salesforce.util;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.CUSTOM_FIELDS;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.FIELDS;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.OBJECT;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.Q;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class SalesforceUtils {

    private SalesforceUtils() {
    }

    public static FileEntry combineFieldsAndCreateJsonFile(Parameters inputParameters, ActionContext actionContext) {
        Map<String, Object> fields = inputParameters.getRequiredMap(FIELDS, Object.class);
        Map<String, Object> customFields = inputParameters.getMap(CUSTOM_FIELDS, Object.class, Map.of());

        fields.putAll(customFields);

        String jsonString = actionContext.json(json -> json.write(fields));

        return actionContext.file(file -> file.storeContent("new.json", jsonString));
    }

    public static List<? extends ValueProperty<?>> createPropertiesForObject(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext actionContext) {

        List<ValueProperty<?>> list = new ArrayList<>();

        String object = inputParameters.getRequiredString(OBJECT);

        Map<String, Object> body = getSObjectDescribe(actionContext, object);

        if (body.get(FIELDS) instanceof List<?> fields) {
            for (Object field : fields) {
                if (field instanceof Map<?, ?> map) {
                    String name = (String) map.get("name");
                    String label = (String) map.get("label");
                    String type = (String) map.get("type");

                    FieldType fieldType = FieldType.getFieldType(type);

                    if (fieldType != null) {
                        switch (fieldType) {
                            case BOOLEAN -> list.add(
                                bool(name)
                                    .label(label)
                                    .required(false));
                            case DATE -> list.add(
                                date(name)
                                    .label(label)
                                    .required(false));
                            case DATETIME -> list.add(
                                dateTime(name)
                                    .label(label)
                                    .required(false));
                            case DOUBLE, LONG, PERCENT -> list.add(
                                number(name)
                                    .label(label)
                                    .required(false));
                            case INT -> list.add(
                                integer(name)
                                    .label(label)
                                    .required(false));
                            case STRING -> list.add(
                                string(name)
                                    .label(label)
                                    .required(false));
                            case TIME -> list.add(
                                time(name)
                                    .label(label)
                                    .required(false));
                            case ADDRESS -> list.add(
                                object(name)
                                    .label(label)
                                    .additionalProperties()
                                    .required(false));
                            case EMAIL -> list.add(
                                string(name)
                                    .label(label)
                                    .controlType(ControlType.EMAIL)
                                    .required(false));
                            case PICKLIST -> {
                                List<Option<String>> options = new ArrayList<>();

                                if (map.get("picklistValues") instanceof List<?> picklistValues) {
                                    for (Object picklistValue : picklistValues) {
                                        if (picklistValue instanceof Map<?, ?> picklistValueMap) {
                                            Object active = picklistValueMap.get("active");

                                            if (active.equals(true)) {
                                                options.add(
                                                    option(
                                                        (String) picklistValueMap.get("label"),
                                                        (String) picklistValueMap.get("value")));
                                            }
                                        }
                                    }
                                }

                                list.add(
                                    string(name)
                                        .label(label)
                                        .options(options)
                                        .required(false));
                            }
                            case TEXTAREA -> list.add(
                                string(name)
                                    .label(label)
                                    .controlType(ControlType.TEXT_AREA)
                                    .required(false));
                            case URL -> list.add(
                                string(name)
                                    .label(label)
                                    .controlType(ControlType.URL)
                                    .required(false));
                            case PHONE -> list.add(
                                string(name)
                                    .label(label)
                                    .controlType(ControlType.PHONE)
                                    .required(false));
                            default -> {
                            }
                        }
                    }
                }
            }
        }

        return list;
    }

    public static Map<String, ?> executeSOQLQuery(Context context, String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        return context.http(http -> http.get("/query"))
            .queryParameter(Q, encodedQuery)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static PollOutput getPollOutput(
        Parameters inputParameters, Parameters closureParameters, TriggerContext triggerContext, String dateFieldName) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, now.minusHours(3));

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(zoneId));
        String query = String.format(
            "SELECT FIELDS(ALL) FROM %s WHERE %s > %s ORDER BY %s ASC LIMIT 200 OFFSET 0",
            inputParameters.getRequiredString(OBJECT), dateFieldName, formattedStartDate, dateFieldName);

        Map<String, ?> responseBody = executeSOQLQuery(triggerContext, query);

        if (responseBody.get("records") instanceof List<?> records) {
            return new PollOutput(records, Map.of(LAST_TIME_CHECKED, now), false);
        }

        throw new ProviderException("Failed to fetch records from Salesforce.");
    }

    public static List<Option<String>> getRecordIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        String query = "SELECT Id FROM " + inputParameters.getRequiredString(OBJECT);

        Map<String, ?> body = executeSOQLQuery(actionContext, query);

        List<Option<String>> options = new ArrayList<>();

        if (body.get("records") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String id = (String) map.get("Id");

                    options.add(option(id, id));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getSalesforceObjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, ?> body = context.http(http -> http.get("/sobjects"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("sobjects") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("label"), (String) map.get("name")));
                }
            }
        }

        return options;
    }

    private static Map<String, Object> getSObjectDescribe(ActionContext actionContext, String object) {
        return actionContext
            .http(http -> http.get("/sobjects/" + object + "/describe"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
