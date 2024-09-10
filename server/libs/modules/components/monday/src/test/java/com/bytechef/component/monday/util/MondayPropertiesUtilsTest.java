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
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.END_DATE;
import static com.bytechef.component.monday.constant.MondayConstants.FROM;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.LABELS;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.START_DATE;
import static com.bytechef.component.monday.constant.MondayConstants.TEXT;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.TO;
import static com.bytechef.component.monday.constant.MondayConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.monday.constant.MondayColumnType;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MondayPropertiesUtilsTest {

    private static final String LABEL = "label";

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters parameters = MockParametersFactory.create(Map.of(BOARD_ID, "board"));

    @Test
    void testCreatePropertiesForItem() {
        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic.when(() -> MondayUtils.getBoardColumns("board", mockedActionContext))
                .thenReturn(List.of(
                    Map.of(TYPE, MondayColumnType.CHECKBOX.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.COUNTRY.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.DATE.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(
                        TYPE, MondayColumnType.DROPDOWN.getName(), ID, NAME, TITLE,
                        LABEL, "settings_str", "{\"labels\":[{\"name\":\"item1\"}] }"),
                    Map.of(TYPE, MondayColumnType.EMAIL.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.LINK.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.TEXT.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.HOUR.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.LOCATION.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.LONG_TEXT.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.NUMBERS.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.RATING.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(
                        TYPE, MondayColumnType.STATUS.getName(), ID, NAME, TITLE,
                        LABEL, "settings_str", "{\"labels\":{1:\"status1\"}}"),
                    Map.of(TYPE, MondayColumnType.TIMELINE.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.WEEK.getName(), ID, NAME, TITLE, LABEL),
                    Map.of(TYPE, MondayColumnType.WORLD_CLOCK.getName(), ID, NAME, TITLE, LABEL)));

            when(mockedActionContext.json(any())).thenReturn(
                Map.of("CAN", "Canada"),
                Map.of(LABELS, List.of(Map.of(NAME, "item1"))),
                Map.of(LABELS, Map.of(1, "status1")));

            List<ValueProperty<?>> propertiesForItem = MondayPropertiesUtils.createPropertiesForItem(
                parameters, parameters, Map.of(), mockedActionContext);

            assertEquals(getExpectedProperties(), propertiesForItem);
        }
    }

    private static List<? extends ModifiableValueProperty<?, ? extends ModifiableValueProperty<?, ?>>>
        getExpectedProperties() {

        return List.of(
            bool(NAME)
                .label(LABEL)
                .required(false),
            string(NAME)
                .label(LABEL)
                .options(option("Canada", "CAN-Canada"))
                .required(false),
            date(NAME)
                .label(LABEL)
                .required(false),
            array(NAME)
                .label(LABEL)
                .items(string().options(option("item1", "item1")))
                .required(false),
            string(NAME)
                .label(LABEL)
                .required(false),
            string(NAME)
                .label(LABEL)
                .required(false),
            string(NAME)
                .label(LABEL)
                .required(false),
            time(NAME)
                .label(LABEL)
                .required(false),
            object(NAME)
                .label(LABEL)
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
                .required(false),
            string(NAME)
                .label(LABEL)
                .controlType(Property.ControlType.TEXT)
                .required(false),
            number(NAME)
                .label(LABEL)
                .required(false),
            integer(NAME)
                .label(LABEL)
                .description("A number between 1 and 5.")
                .options(
                    option("1", 1),
                    option("2", 2),
                    option("3", 3),
                    option("4", 4),
                    option("5", 5))
                .required(false),
            string(NAME)
                .label(LABEL)
                .options(option("status1", "status1"))
                .required(false),
            object(NAME)
                .label(LABEL)
                .properties(
                    date(FROM)
                        .label("Start Date")
                        .required(true),
                    date(TO)
                        .label("End Date")
                        .required(true))
                .required(false),
            object(NAME)
                .label(LABEL)
                .description(
                    "Enter the start and end date. The dates must be 7 days apart (inclusive of the first and last date).")
                .properties(
                    date(START_DATE)
                        .label("Start date")
                        .required(true),
                    date(END_DATE)
                        .label("End date")
                        .required(true))
                .required(false),
            string(NAME)
                .label(LABEL)
                .description("Enter the timezone in the 'Continent/City' format.")
                .exampleValue("Europe/London")
                .required(false));
    }

    @Test
    void testConvertPropertyToMondayColumnValue() {
        Map<String, Object> columnValuesInput = new HashMap<>();

        columnValuesInput.put("checkboxKey", true);
        columnValuesInput.put("countryKey", "US-United States");
        columnValuesInput.put("dateKey", "2023-10-01");
        columnValuesInput.put("dropdownKey", List.of("a"));
        columnValuesInput.put("emailKey", "test@mail.com");
        columnValuesInput.put("hourKey", "05:20");
        columnValuesInput.put("linkKey", "testLink");
        columnValuesInput.put("longTextKey", "text text 123");
        columnValuesInput.put("locationKey", Map.of());
        columnValuesInput.put("textKey", "text123");
        columnValuesInput.put("timelineKey", Map.of());
        columnValuesInput.put("numbersKey", 123.1);
        columnValuesInput.put("ratingKey", 2);
        columnValuesInput.put("statusKey", "status");
        columnValuesInput.put("weekKey", Map.of());
        columnValuesInput.put("worldClockKey", "Europe/Zagreb");

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic.when(
                () -> MondayUtils.getBoardColumns("board", mockedActionContext))
                .thenReturn(List.of(Map.of(ID, "checkboxKey", TYPE, "checkbox"),
                    Map.of(ID, "countryKey", TYPE, "country"),
                    Map.of(ID, "dateKey", TYPE, "date"),
                    Map.of(ID, "dropdownKey", TYPE, "dropdown"),
                    Map.of(ID, "emailKey", TYPE, "email"),
                    Map.of(ID, "hourKey", TYPE, "hour"),
                    Map.of(ID, "linkKey", TYPE, "link"),
                    Map.of(ID, "longTextKey", TYPE, "long_text"),
                    Map.of(ID, "locationKey", TYPE, "location"),
                    Map.of(ID, "textKey", TYPE, "text"),
                    Map.of(ID, "timelineKey", TYPE, "timeline"),
                    Map.of(ID, "numbersKey", TYPE, "numbers"),
                    Map.of(ID, "ratingKey", TYPE, "rating"),
                    Map.of(ID, "statusKey", TYPE, "status"),
                    Map.of(ID, "weekKey", TYPE, "week"),
                    Map.of(ID, "worldClockKey", TYPE, "world_clock")));

            Map<String, Object> result = MondayPropertiesUtils.convertPropertyToMondayColumnValue(columnValuesInput,
                "board", mockedActionContext);

            assertEquals(16, result.size());
            assertEquals(Map.of("checked", true), result.get("checkboxKey"));
            assertEquals(Map.of("countryCode", "US", "countryName", "United States"), result.get("countryKey"));
            assertEquals(Map.of("date", "2023-10-01"), result.get("dateKey"));
            assertEquals(Map.of(LABELS, List.of("a")), result.get("dropdownKey"));
            assertEquals(Map.of("email", "test@mail.com", TEXT, "test@mail.com"), result.get("emailKey"));
            assertEquals(Map.of("hour", 5, "minute", 20), result.get("hourKey"));
            assertEquals(Map.of("url", "testLink", TEXT, "testLink"), result.get("linkKey"));
            assertEquals(Map.of(TEXT, "text text 123"), result.get("longTextKey"));
            assertEquals(Map.of(), result.get("locationKey"));
            assertEquals("text123", result.get("textKey"));
            assertEquals(Map.of(), result.get("timelineKey"));
            assertEquals("123.1", result.get("numbersKey"));
            assertEquals(Map.of("rating", 2), result.get("ratingKey"));
            assertEquals(Map.of("label", "status"), result.get("statusKey"));
            assertEquals(Map.of("week", Map.of()), result.get("weekKey"));
            assertEquals(Map.of("timezone", "Europe/Zagreb"), result.get("worldClockKey"));
        }
    }
}
