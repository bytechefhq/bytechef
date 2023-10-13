/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.ComponentPropertiesFunction;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.Context.Http.ResponseType;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableOption;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableValueProperty;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property.ControlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class AirtableUtils {

    private static final Logger logger = LoggerFactory.getLogger(AirtableUtils.class);

    private static final List<String> SKIP_FIELDS = List.of("singleCollaborator", "multipleCollaborators");

    public static ComponentOptionsFunction getBaseIdOptions() {
        return (inputParameters, connectionParameters, searchText, context) -> {
            Map<String, List<Map<?, ?>>> response = context
                .http(http -> http.get("https://api.airtable.com/v0/meta/bases"))
                .configuration(Http.responseType(ResponseType.JSON))
                .execute()
                .getBody();

            if (logger.isDebugEnabled()) {
                logger.debug("Response for url='https://api.airtable.com/v0/meta/bases': " + response);
            }

            return getOptions(response, "bases");
        };
    }

    public static ComponentPropertiesFunction getFieldsProperties() {
        return (inputParameters, connection, context) -> {
            List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

            String url = "https://api.airtable.com/v0/meta/bases/%s/tables".formatted(
                inputParameters.getRequiredString(BASE_ID));

            Map<String, List<AirtableTable>> tablesMap = context.json(json -> json.read(
                (String) context.http(http -> http.get(url)
                    .configuration(Http.responseType(ResponseType.TEXT))
                    .execute()
                    .getBody()),
                new Context.TypeReference<>() {}));

            if (logger.isDebugEnabled()) {
                logger.debug("Response for url='%s': %s".formatted(url, tablesMap));
            }

            List<AirtableTable> tables = tablesMap.get("tables");

            AirtableTable table = tables
                .stream()
                .filter(curTable -> Objects.equals(
                    curTable.id(), inputParameters.getRequiredString(TABLE_ID)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Request Airtable table does not exist"));

            for (AirtableField field : table.fields()) {
                if (SKIP_FIELDS.contains(field.type())) {
                    continue;
                }

                ModifiableValueProperty<?, ?> property = switch (field.type()) {
                    case "autoNumber", "percent", "count" -> integer(field.name());
                    case "barcode", "button", "createdBy", "createdTime", "currency", "externalSyncSource", "formula",
                        "lastModifiedBy", "lastModifiedTime", "lookup", "multipleAttachments", "multipleLookupValues",
                        "multipleRecordLinks", "rating", "richText", "rollup", "singleLineText", "date", "dateTime",
                        "duration" -> string(field.name());
                    case "checkbox" -> bool(field.name());
                    case "email" -> string(field.name())
                        .controlType(ControlType.EMAIL);
                    case "multilineText" -> string(field.name())
                        .controlType(ControlType.TEXT_AREA);
                    case "multipleSelects" -> array(field.name())
                        .items(string())
                        .options(getOptions(field));
                    case "number" -> number(field.name());
                    case "phoneNumber" -> string(field.name())
                        .controlType(ControlType.PHONE);
                    case "singleSelect" -> string(field.name()).options(
                        getOptions(field));
                    case "url" -> string(field.name()).controlType(ControlType.URL);
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
                ComponentDSL.object("__item")
                    .label("Record")
                    .properties(
                        ComponentDSL.object("fields")
                            .label("Fields")
                            .properties(properties)
                            .required(false)));
        };
    }

    public static ComponentOptionsFunction getTableIdOptions() {
        return (inputParameters, connectionParameters, searchText, context) -> {
            String url = "https://api.airtable.com/v0/meta/bases/%s/tables".formatted(
                inputParameters.getRequiredString(BASE_ID));

            Map<String, List<Map<?, ?>>> response = context.http(http -> http.get(url)
                .configuration(Http.responseType(ResponseType.JSON))
                .execute()
                .getBody());

            if (logger.isDebugEnabled()) {
                logger.debug("Response for url='%s': %s".formatted(url, response));
            }

            return getOptions(response, "tables");
        };
    }

    private static List<Option<?>> getOptions(Map<String, List<Map<?, ?>>> response, String name) {
        List<Option<?>> options = new ArrayList<>();

        for (Map<?, ?> list : response.get(name)) {
            options.add(option((String) list.get("name"), list.get("id")));
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

    private record AirtableChoice(String id, String name, String color) {
    }

    private record AirtableField(
        String id, String name, String description, String type, AirtableOptions options) {
    }

    private record AirtableOptions(List<AirtableChoice> choices) {
    }

    private record AirtableTable(
        String id, String name, List<AirtableField> fields, String description, String primaryFieldId,
        List<AirtableTableView> views) {
    }

    private record AirtableTableView(String id, String name, String type) {
    }
}
