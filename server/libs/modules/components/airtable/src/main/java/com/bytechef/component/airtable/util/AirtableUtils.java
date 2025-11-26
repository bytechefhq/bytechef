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

package com.bytechef.component.airtable.util;

import static com.bytechef.component.airtable.constant.AirtableConstants.BASE_ID;
import static com.bytechef.component.airtable.constant.AirtableConstants.OFFSET;
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableOption;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class AirtableUtils extends AbstractAirtableUtils {

    private AirtableUtils() {
    }

    private static final List<String> SKIP_FIELDS = List.of("singleCollaborator", "multipleCollaborators");

    public static List<Object> getAllRecords(
        Context context, String baseId, String tableId, boolean editorEnvironment,
        Object... additionalQueryParameters) {

        List<Object> records = new ArrayList<>();
        String offset = null;
        int pageSize = editorEnvironment ? 1 : 100;

        do {
            List<Object> queryParameters = new ArrayList<>();

            queryParameters.add("pageSize");
            queryParameters.add(pageSize);
            queryParameters.add(OFFSET);
            queryParameters.add(offset);

            queryParameters.addAll(List.of(additionalQueryParameters));

            Map<String, ?> body = context.http(http -> http.get("/%s/%s".formatted(baseId, tableId)))
                .queryParameters(queryParameters.toArray())
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("records") instanceof List<?> list) {
                records.addAll(list);
            }

            if (editorEnvironment) {
                break;
            }

            offset = (String) body.get(OFFSET);
        } while (offset != null);

        return records;
    }

    public static List<Option<String>> getBaseIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        List<Object> bases = new ArrayList<>();
        String offset = null;

        do {
            Map<String, ?> body = context
                .http(http -> http.get("/meta/bases"))
                .queryParameters(OFFSET, offset)
                .configuration(Http.responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            context.log(
                logger -> logger.debug("Response for url='https://api.airtable.com/v0/meta/bases': " + body));

            if (body.get("bases") instanceof List<?> list) {
                bases.addAll(list);
            }

            offset = (String) body.get(OFFSET);
        } while (offset != null);

        for (Object o : bases) {
            if (o instanceof Map<?, ?> map) {
                options.add(option((String) map.get("name"), (String) map.get("id")));
            }
        }

        return options;
    }

    public static List<ModifiableValueProperty<?, ?>> getFieldsProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        Context context) {

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        String url = "/meta/bases/%s/tables".formatted(
            inputParameters.getRequiredString(BASE_ID));

        Http.Response response = context.http(http -> http.get(url))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute();

        Map<?, ?> body = response.getBody(Map.class);

        if (body.containsKey("error")) {
            throw new ProviderException.BadRequestException(
                (String) ((Map<?, ?>) body.get("error")).get("message"));
        }

        Map<String, List<AirtableTable>> tablesMap = response.getBody(new TypeReference<>() {});

        context.log(log -> log.debug("Response for url='%s': %s".formatted(url, tablesMap)));

        List<AirtableTable> tables = tablesMap.get("tables");

        AirtableTable table = tables
            .stream()
            .filter(curTable -> Objects.equals(
                curTable.id(), inputParameters.getRequiredString(TABLE_ID)))
            .findFirst()
            .orElseThrow(() -> new ProviderException.BadRequestException("Requested table does not exist"));

        for (AirtableField field : table.fields()) {
            if (SKIP_FIELDS.contains(field.type())) {
                continue;
            }

            String name = field.name();

            ModifiableValueProperty<?, ?> property = switch (field.type()) {
                case "autoNumber", "percent", "count" -> integer(name);
                case "barcode", "button", "createdBy", "createdTime", "currency", "externalSyncSource", "formula",
                    "lastModifiedBy", "lastModifiedTime", "lookup", "multipleAttachments", "multipleLookupValues",
                    "multipleRecordLinks", "rating", "richText", "rollup", "singleLineText", "date", "dateTime",
                    "duration" -> string(name);
                case "checkbox" -> bool(name);
                case "email" -> string(name)
                    .controlType(ControlType.EMAIL);
                case "multilineText" -> string(name)
                    .controlType(ControlType.TEXT_AREA);
                case "multipleSelects" -> array(name)
                    .items(string())
                    .options(getOptions(field));
                case "number" -> number(name);
                case "phoneNumber" -> string(name)
                    .controlType(ControlType.PHONE);
                case "singleSelect" -> string(name).options(getOptions(field));
                case "url" -> string(name).controlType(ControlType.URL);
                default -> throw new IllegalArgumentException(
                    "Unknown Airtable field type='%s'".formatted(field.type()));
            };

            properties.add(
                property.description(
                    List.of("date", "dateTime")
                        .contains(field.type())
                            ? "%s. Expected format for value: mmmm d,yyyy".formatted(field.description())
                            : field.description()));
        }

        return List.of(
            object("fields")
                .label("Fields")
                .properties(properties)
                .required(false));
    }

    public static List<Option<String>> getRecordIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        List<Object> records = getAllRecords(
            context, inputParameters.getRequiredString(BASE_ID), inputParameters.getRequiredString(TABLE_ID), false);

        for (Object record : records) {
            if (record instanceof Map<?, ?> map) {
                options.add(option((String) map.get("id"), (String) map.get("id")));
            }
        }

        return options;
    }

    public static List<Option<String>> getTableIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        String url = "/meta/bases/%s/tables".formatted(inputParameters.getRequiredString(BASE_ID));

        Map<String, ?> body = context.http(http -> http.get(url))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.containsKey("error")) {
            throw new ProviderException.BadRequestException((String) ((Map<?, ?>) body.get("error")).get("message"));
        }

        context.log(log -> log.debug("Response for url='%s': %s".formatted(url, body)));

        if (body.get("tables") instanceof List<?> list) {

            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

    private static List<ModifiableOption<String>> getOptions(AirtableField field) {
        if (field.options() == null) {
            return null;
        }

        AirtableOptions options = field.options();

        if (options.choices() == null) {
            return null;
        }

        return options
            .choices()
            .stream()
            .map(choice -> option(choice.name(), choice.id()))
            .toList();
    }

    protected record AirtableChoice(String id, String name, String color) {
    }

    protected record AirtableField(
        String id, String name, String description, String type, AirtableOptions options) {
    }

    @SuppressFBWarnings("EI")
    protected record AirtableOptions(List<AirtableChoice> choices) {
    }

    @SuppressFBWarnings("EI")
    protected record AirtableTable(
        String id, String name, List<AirtableField> fields, String description, String primaryFieldId,
        List<AirtableTableView> views) {
    }

    private record AirtableTableView(String id, String name, String type) {
    }
}
