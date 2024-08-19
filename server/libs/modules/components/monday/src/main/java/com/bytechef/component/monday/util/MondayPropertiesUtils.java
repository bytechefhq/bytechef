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

package com.bytechef.component.monday.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;
import static com.bytechef.component.monday.constant.MondayColumnType.getColumnTypeByName;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.END_DATE;
import static com.bytechef.component.monday.constant.MondayConstants.FROM;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.LABELS;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.NOT_WRITABLE_COLUMN_TYPES;
import static com.bytechef.component.monday.constant.MondayConstants.START_DATE;
import static com.bytechef.component.monday.constant.MondayConstants.TEXT;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.TO;
import static com.bytechef.component.monday.constant.MondayConstants.TYPE;
import static com.bytechef.component.monday.util.MondayUtils.getBoardColumns;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableDateProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTimeProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.monday.constant.MondayColumnType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
public class MondayPropertiesUtils {

    public static List<ValueProperty<?>> createPropertiesForItem(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext context) {

        List<?> boardColumns = getBoardColumns(inputParameters.getRequiredString(BOARD_ID), context);

        return new ArrayList<>(boardColumns.stream()
            .filter(o -> o instanceof Map<?, ?>)
            .map(o -> createProperty((Map<?, ?>) o, context))
            .filter(Objects::nonNull)
            .toList());
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<?, ?> columnMap, ActionContext actionContext) {
        String type = (String) columnMap.get(TYPE);
        String id = (String) columnMap.get(ID);
        String title = (String) columnMap.get(TITLE);

        MondayColumnType enumType = getColumnTypeByName(type);

        if (!NOT_WRITABLE_COLUMN_TYPES.contains(enumType)) {
            return switch (Objects.requireNonNull(enumType)) {
                case CHECKBOX -> createPropertyForCheckboxColumnType(id, title);
                case COUNTRY -> createPropertyForCountryColumnType(id, title, actionContext);
                case DATE -> createPropertyForDateColumType(id, title);
                case DROPDOWN -> createPropertyForDropdownColumnType(
                    id, title, (String) columnMap.get("settings_str"), actionContext);
                case EMAIL, LINK, TEXT -> createPropertyForEmailTextAndLinkColumnType(id, title);
                case HOUR -> createPropertyForHourColumnType(id, title);
                case LOCATION -> createPropertyForLocationColumnType(id, title);
                case LONG_TEXT -> createPropertyForLongTextColumnType(id, title);
                case NUMBERS -> createPropertyForNumbersColumnType(id, title);
                case RATING -> createPropertyForRatingColumnType(id, title);
                case STATUS ->
                    createPropertyForStatusColumnType(id, title, (String) columnMap.get("settings_str"), actionContext);
                case TIMELINE -> createPropertyForTimelineColumnType(id, title);
                case WEEK -> createPropertyForWeekColumnType(id, title);
                case WORLD_CLOCK -> createPropertyForWorldClockColumnType(id, title);
                default -> null;
            };
        }

        return null;
    }

    private static ModifiableBooleanProperty createPropertyForCheckboxColumnType(String id, String title) {
        return bool(id)
            .label(title)
            .required(false);
    }

    private static ModifiableStringProperty createPropertyForCountryColumnType(
        String id, String title, ActionContext actionContext) {

        InputStream inputStream = MondayPropertiesUtils.class.getClassLoader()
            .getResourceAsStream("assets/country.json");
        Map<String, String> countryMap = actionContext.json(json -> json.readMap(inputStream, String.class));

        List<Option<String>> options = new ArrayList<>();

        countryMap.forEach((key, value) -> options.add(option(value, key + "-" + value)));

        return string(id)
            .label(title)
            .options(options)
            .required(false);
    }

    private static ModifiableDateProperty createPropertyForDateColumType(String id, String title) {
        return date(id)
            .label(title)
            .required(false);
    }

    private static ModifiableArrayProperty createPropertyForDropdownColumnType(
        String id, String title, String settingsStr, ActionContext actionContext) {

        Map<String, ?> settingStrMap = actionContext.json(json -> json.readMap(settingsStr));

        List<Option<String>> options = new ArrayList<>();

        if (settingStrMap.get(LABELS) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String name = (String) map.get(NAME);

                    options.add(option(name, name));
                }
            }
        }

        return array(id)
            .label(title)
            .items(string().options(options))
            .required(false);
    }

    private static ModifiableStringProperty createPropertyForEmailTextAndLinkColumnType(String id, String title) {
        return string(id)
            .label(title)
            .required(false);
    }

    private static ModifiableTimeProperty createPropertyForHourColumnType(String id, String title) {
        return time(id)
            .label(title)
            .required(false);
    }

    private static ModifiableObjectProperty createPropertyForLocationColumnType(String id, String title) {
        return object(id)
            .label(title)
            .properties(
                number("lat")
                    .label("Latitude")
                    .required(true),
                number("lng")
                    .label("Longitude")
                    .required(true),
                string("address")
                    .label("Address")
                    .required(false))
            .required(false);
    }

    private static ModifiableStringProperty createPropertyForLongTextColumnType(String id, String title) {
        return string(id)
            .label(title)
            .controlType(ControlType.TEXT)
            .required(false);
    }

    private static ModifiableNumberProperty createPropertyForNumbersColumnType(String id, String title) {
        return number(id)
            .label(title)
            .required(false);
    }

    private static ModifiableIntegerProperty createPropertyForRatingColumnType(String id, String title) {
        return integer(id)
            .label(title)
            .description("A number between 1 and 5.")
            .options(
                option("1", 1),
                option("2", 2),
                option("3", 3),
                option("4", 4),
                option("5", 5))
            .required(false);
    }

    private static ModifiableStringProperty createPropertyForStatusColumnType(
        String id, String title, String settingsStr, ActionContext actionContext) {

        Map<String, ?> settingStrMap = actionContext.json(json -> json.readMap(settingsStr));

        Object labels = settingStrMap.get(LABELS);

        List<Option<String>> options = new ArrayList<>();
        if (labels instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String status = (String) entry.getValue();

                options.add(option(status, status));
            }
        }

        return string(id)
            .label(title)
            .options(options)
            .required(false);
    }

    private static ModifiableObjectProperty createPropertyForTimelineColumnType(String id, String title) {
        return object(id)
            .label(title)
            .properties(
                date(FROM)
                    .label("Start Date")
                    .required(true),
                date(TO)
                    .label("End Date")
                    .required(true))
            .required(false);
    }

    private static ModifiableObjectProperty createPropertyForWeekColumnType(String id, String title) {
        return object(id)
            .label(title)
            .description(
                "Enter the start and end date. The dates must be 7 days apart (inclusive of the first and last date).")
            .properties(
                date(START_DATE)
                    .label("Start date")
                    .required(true),
                date(END_DATE)
                    .label("End date")
                    .required(true))
            .required(false);
    }

    private static ModifiableStringProperty createPropertyForWorldClockColumnType(String id, String title) {
        return string(id)
            .label(title)
            .description("Enter the timezone in the 'Continent/City' format.")
            .exampleValue("Europe/London")
            .required(false);
    }

    public static Map<String, Object> convertPropertyToMondayColumnValue(
        Map<String, ?> columnValuesInput, String boardId, ActionContext actionContext) {

        Map<String, Object> mondayColumnValues = new HashMap<>();

        for (Map.Entry<String, ?> entry : columnValuesInput.entrySet()) {
            String key = entry.getKey();

            if (!Objects.equals(key, "")) {
                Map<String, String> columnIdTypeMap = generateColumnIdTypeMap(boardId, actionContext);

                MondayColumnType enumType = getColumnTypeByName(columnIdTypeMap.get(key));

                final Object value = entry.getValue();

                switch (enumType) {
                    case CHECKBOX -> mondayColumnValues.put(key, Map.of("checked", value));
                    case COUNTRY -> {
                        String[] country = ((String) value).split("-");
                        mondayColumnValues.put(key, Map.of("countryCode", country[0], "countryName", country[1]));
                    }
                    case DATE -> mondayColumnValues.put(key, Map.of("date", value));
                    case DROPDOWN -> mondayColumnValues.put(key, Map.of(LABELS, value));
                    case EMAIL -> mondayColumnValues.put(key, Map.of("email", value, TEXT, value));
                    case HOUR -> {
                        String[] time = ((String) value).split(":");
                        mondayColumnValues.put(key,
                            Map.of("hour", Integer.valueOf(time[0]), "minute", Integer.valueOf(time[1])));
                    }
                    case LINK -> mondayColumnValues.put(key, Map.of("url", value, TEXT, value));
                    case LONG_TEXT -> mondayColumnValues.put(key, Map.of(TEXT, value));
                    case LOCATION, TEXT, TIMELINE -> mondayColumnValues.put(key, value);
                    case NUMBERS -> mondayColumnValues.put(key, value
                        .toString());
                    case RATING -> mondayColumnValues.put(key, Map.of("rating", value));
                    case STATUS -> mondayColumnValues.put(key, Map.of("label", value));
                    case WEEK -> mondayColumnValues.put(key, Map.of("week", value));
                    case WORLD_CLOCK -> mondayColumnValues.put(key, Map.of("timezone", value));
                    default -> {
                    }
                }
            }
        }

        return mondayColumnValues;
    }

    private static Map<String, String> generateColumnIdTypeMap(String boardId, ActionContext actionContext) {
        List<?> boardColumns = getBoardColumns(boardId, actionContext);

        Map<String, String> columnIdTypeMap = new HashMap<>();

        for (Object o : boardColumns) {
            if (o instanceof Map<?, ?> map) {
                columnIdTypeMap.put((String) map.get(ID), (String) map.get(TYPE));
            }
        }

        return columnIdTypeMap;
    }
}
