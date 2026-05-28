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
import static com.bytechef.component.aitable.constant.AITableConstants.NAME;
import static com.bytechef.component.aitable.constant.AITableConstants.SPACE_ID;
import static com.bytechef.component.aitable.constant.AITableConstants.TYPE;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.CHECKBOX;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.CURRENCY;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.DATE_TIME;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.EMAIL;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.MEMBER;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.MULTI_SELECT;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.NUMBER;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.PERCENT;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.PHONE;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.RATING;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.SINGLE_SELECT;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.SINGLE_TEXT;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.TEXT;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.TWO_WAY_LINK;
import static com.bytechef.component.aitable.util.AITableUtils.FieldType.URL;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class AITableUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testCreatePropertiesForRecord(
        ActionContext mockedContext, Executor mockedExecutor, Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(DATASHEET_ID, "abc"));

        List<Map<String, Object>> fields = new ArrayList<>();

        fields.add(createFieldMap("checkbox", CHECKBOX.getName(), Map.of()));
        fields.add(createFieldMap("rating", RATING.getName(), Map.of("max", 6)));
        fields.add(createFieldMap("longText", TEXT.getName(), Map.of()));
        fields.add(
            createFieldMap("singleSelect", SINGLE_SELECT.getName(),
                Map.of("options", List.of(Map.of("name", "option1")))));
        fields.add(createFieldMap("number", NUMBER.getName(), Map.of("precision", 2)));
        fields.add(createFieldMap("singleText", SINGLE_TEXT.getName(), Map.of()));
        fields.add(createFieldMap("url", URL.getName(), Map.of()));
        fields.add(createFieldMap("phone", PHONE.getName(), Map.of()));
        fields.add(createFieldMap("dateOnly", DATE_TIME.getName(), Map.of("includeTime", false)));
        fields.add(createFieldMap("dateTime", DATE_TIME.getName(), Map.of("includeTime", true)));
        fields.add(
            createFieldMap("multiSelect", MULTI_SELECT.getName(),
                Map.of("options", List.of(Map.of("name", "option1")))));
        fields.add(createFieldMap("currency", CURRENCY.getName(), Map.of("symbol", "$", "precision", 2)));
        fields.add(createFieldMap("percent", PERCENT.getName(), Map.of("precision", 2)));
        fields.add(createFieldMap("email", EMAIL.getName(), Map.of()));
        fields.add(createFieldMap("member", MEMBER.getName(),
            Map.of("options", List.of(Map.of("name", "Alice", "id", "u1")))));
        fields.add(createFieldMap("twoWayLink", TWO_WAY_LINK.getName(), Map.of()));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of("fields", fields)));

        List<? extends ValueProperty<?>> result = AITableUtils.createPropertiesForRecord(
            mockedParameters, null, null, mockedContext);

        assertEquals(getExpectedProperties(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/datasheets/abc/fields", stringArgumentCaptor.getValue());
    }

    private static List<? extends ValueProperty<?>> getExpectedProperties() {
        return List.of(
            bool("checkbox")
                .label("checkbox")
                .required(false),
            integer("rating")
                .label("rating")
                .maxValue(6)
                .required(false),
            string("longText")
                .label("longText")
                .controlType(ControlType.TEXT_AREA)
                .required(false),
            string("singleSelect")
                .label("singleSelect")
                .options(option("option1", "option1"))
                .required(false),
            number("number")
                .label("number")
                .maxNumberPrecision(2)
                .required(false),
            string("singleText")
                .label("singleText")
                .required(false),
            string("url")
                .label("url")
                .required(false),
            string("phone")
                .label("phone")
                .controlType(ControlType.PHONE)
                .required(false),
            date("dateOnly")
                .label("dateOnly")
                .required(false),
            dateTime("dateTime")
                .label("dateTime")
                .required(false),
            array("multiSelect")
                .label("multiSelect")
                .items(string())
                .options(List.of(option("option1", "option1")))
                .required(false),
            number("currency")
                .label("currency")
                .description("Currency symbol: $")
                .maxNumberPrecision(2)
                .required(false),
            number("percent")
                .label("percent")
                .maxNumberPrecision(2)
                .required(false),
            string("email")
                .label("email")
                .required(false),
            array("member")
                .label("member")
                .options(List.of(option("Alice", "u1")))
                .items(string())
                .required(false),
            array("twoWayLink")
                .label("twoWayLink")
                .required(false));
    }

    private static Map<String, Object> createFieldMap(String name, String type, Map<String, Object> property) {
        Map<String, Object> map = new HashMap<>();

        map.put(NAME, name);
        map.put(TYPE, type);
        map.put("property", property);

        return map;
    }

    @Test
    void testGetDatasheetIdOptions(
        ActionContext mockedContext, Executor mockedExecutor, Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(SPACE_ID, "abc"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of("nodes", List.of(Map.of("name", "name", "id", "id")))));

        List<Option<String>> result = AITableUtils.getDatasheetIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(List.of(option("name", "id")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/spaces/abc/nodes", stringArgumentCaptor.getValue());

        Object[] queryParameters = {
            TYPE, "Datasheet"
        };

        assertArrayEquals(queryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetFieldNamesOptions(
        ActionContext mockedContext, Executor mockedExecutor, Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(DATASHEET_ID, "abc"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(FIELDS, List.of(Map.of("name", "name")))));

        List<Option<String>> result = AITableUtils.getFieldNamesOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(List.of(option("name", "name")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/datasheets/abc/fields", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetSpaceIdOptions(
        ActionContext mockedContext, Executor mockedExecutor, Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of("spaces", List.of(Map.of("name", "name", "id", "id")))));

        List<Option<String>> result = AITableUtils.getSpaceIdOptions(null, null, null, null, mockedContext);

        assertEquals(List.of(option("name", "id")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/spaces", stringArgumentCaptor.getValue());
    }
}
