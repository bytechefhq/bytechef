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

package com.bytechef.component.baserow.util;

import static com.bytechef.component.baserow.constant.BaserowConstants.NAME;
import static com.bytechef.component.baserow.constant.BaserowConstants.TABLE_ID;
import static com.bytechef.component.baserow.constant.BaserowConstants.TYPE;
import static com.bytechef.component.baserow.util.BaserowPropertiesUtils.READ_ONLY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.baserow.constant.BaserowFieldType;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class BaserowPropertiesUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(TABLE_ID, "table"));

    @Test
    void testCreatePropertiesForNewRow() {
        try (MockedStatic<BaserowUtils> baserowUtilsMockedStatic = mockStatic(BaserowUtils.class)) {
            baserowUtilsMockedStatic.when(() -> BaserowUtils.getTableFields(mockedActionContext, "table"))
                .thenReturn(List.of(
                    Map.of(TYPE, BaserowFieldType.BOOLEAN.getName(), NAME, NAME, READ_ONLY, false),
                    Map.of(TYPE, BaserowFieldType.RATING.getName(), NAME, NAME, READ_ONLY, false, "max_value", 6),
                    Map.of(TYPE, BaserowFieldType.LONG_TEXT.getName(), NAME, NAME, READ_ONLY, false),
                    Map.of(
                        TYPE, BaserowFieldType.SINGLE_SELECT.getName(), NAME, NAME, READ_ONLY, false, "select_options",
                        List.of(Map.of("value", "option1"))),
                    Map.of(TYPE, BaserowFieldType.NUMBER.getName(), NAME, NAME, READ_ONLY, false),
                    Map.of(TYPE, BaserowFieldType.TEXT.getName(), NAME, NAME, READ_ONLY, false, "text_default",
                        "default"),
                    Map.of(TYPE, BaserowFieldType.URL.getName(), NAME, NAME, READ_ONLY, false),
                    Map.of(TYPE, BaserowFieldType.PHONE_NUMBER.getName(), NAME, NAME, READ_ONLY, false),
                    Map.of(TYPE, BaserowFieldType.DATE.getName(), NAME, NAME, READ_ONLY, false, "date_include_time",
                        false),
                    Map.of(TYPE, BaserowFieldType.DATE.getName(), NAME, NAME, READ_ONLY, false, "date_include_time",
                        true),
                    Map.of(TYPE, BaserowFieldType.MULTI_SELECT.getName(), NAME, NAME, READ_ONLY, false,
                        "select_options", List.of(Map.of("value", "option1")))));

            List<ValueProperty<?>> propertiesForItem = BaserowPropertiesUtils.createPropertiesForNewRow(
                mockedParameters, mockedParameters, Map.of(), mockedActionContext);

            assertEquals(getExpectedProperties(), propertiesForItem);
        }
    }

    private static List<? extends ModifiableValueProperty<?, ? extends ModifiableValueProperty<?, ?>>>
        getExpectedProperties() {

        return List.of(
            bool(NAME)
                .label(NAME)
                .required(false),
            integer(NAME)
                .label(NAME)
                .description("Enter valid value between 1 and 6")
                .maxValue(6)
                .required(false),
            string(NAME)
                .label(NAME)
                .controlType(ControlType.TEXT_AREA)
                .required(false),
            string(NAME)
                .label(NAME)
                .options(option("option1", "option1"))
                .required(false),
            number(NAME)
                .label(NAME)
                .required(false),
            string(NAME)
                .label(NAME)
                .defaultValue("default")
                .required(false),
            string(NAME)
                .label(NAME)
                .controlType(ControlType.URL)
                .required(false),
            string(NAME)
                .label(NAME)
                .controlType(ControlType.PHONE)
                .required(false),
            date(NAME)
                .label(NAME)
                .required(false),
            dateTime(NAME)
                .label(NAME)
                .required(false),
            array(NAME)
                .label(NAME)
                .items(
                    string()
                        .options(option("option1", "option1")))
                .required(false));
    }
}
