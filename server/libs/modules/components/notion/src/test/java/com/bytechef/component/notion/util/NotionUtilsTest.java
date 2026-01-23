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
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.NAME;
import static com.bytechef.component.notion.constant.NotionConstants.TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class NotionUtilsTest {

    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "xy"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testConvertPropertiesToNotionValues(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Map<String, Object> propertiesSchema = Map.ofEntries(
            Map.entry("cb", Map.of("type", "checkbox")),
            Map.entry("dt", Map.of("type", "date")),
            Map.entry("email", Map.of("type", "email")),
            Map.entry("select", Map.of("type", "select")),
            Map.entry("multi", Map.of("type", "multi_select")),
            Map.entry("status", Map.of("type", "status")),
            Map.entry("number", Map.of("type", "number")),
            Map.entry("phone", Map.of("type", "phone_number")),
            Map.entry("rich", Map.of("type", "rich_text")),
            Map.entry("title", Map.of("type", "title")),
            Map.entry("url", Map.of("type", "url")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("properties", propertiesSchema));

        Map<String, Object> inputFields = Map.ofEntries(
            Map.entry("cb", true),
            Map.entry("dt", "2024-01-01"),
            Map.entry("email", "a@b.com"),
            Map.entry("select", "Option A"),
            Map.entry("multi", List.of("One", "Two")),
            Map.entry("status", "In Progress"),
            Map.entry("number", 42),
            Map.entry("phone", "123"),
            Map.entry("rich", "Hello"),
            Map.entry("title", "My Title"),
            Map.entry("url", "https://example.com"));

        Map<String, Object> result = NotionUtils.convertPropertiesToNotionValues(
            mockedContext, inputFields, "db1");

        Map<String, Object> expected = Map.ofEntries(
            Map.entry("cb", Map.of("checkbox", true)),
            Map.entry("dt", Map.of("date", Map.of("start", "2024-01-01"))),
            Map.entry("email", Map.of("email", "a@b.com")),
            Map.entry("select", Map.of("select", Map.of("name", "Option A"))),
            Map.entry("multi", Map.of("multi_select", List.of(Map.of("name", "One"), Map.of("name", "Two")))),
            Map.entry("status", Map.of("status", Map.of("name", "In Progress"))),
            Map.entry("number", Map.of("number", 42)),
            Map.entry("phone", Map.of("phone_number", "123")),
            Map.entry("rich", Map.of("rich_text", List.of(Map.of(
                "type", TEXT,
                TEXT, Map.of(CONTENT, "Hello"))))),
            Map.entry("title", Map.of("title", List.of(Map.of(
                "type", TEXT,
                TEXT, Map.of(CONTENT, "My Title"))))),
            Map.entry("url", Map.of("url", "https://example.com")));

        assertEquals(expected, result);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/data_sources/db1", stringArgumentCaptor.getValue());
    }

    @Test
    void testCreatePropertiesForDatabaseItem(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Map<String, Object> propertiesSchema = Map.ofEntries(
            Map.entry("cb", Map.of("type", "checkbox")),
            Map.entry("dt", Map.of("type", "date")),
            Map.entry("email", Map.of("type", "email")),
            Map.entry("select",
                Map.of("type", "select", "select", Map.of("options", List.of(Map.of(NAME, "option1"))))),
            Map.entry("multi",
                Map.of("type", "multi_select", "multi_select", Map.of("options", List.of(Map.of(NAME, "option1"))))),
            Map.entry("status",
                Map.of("type", "status", "status", Map.of("options", List.of(Map.of(NAME, "option1"))))),
            Map.entry("number", Map.of("type", "number")),
            Map.entry("phone", Map.of("type", "phone_number")),
            Map.entry("rich", Map.of("type", "rich_text")),
            Map.entry("title", Map.of("type", "title")),
            Map.entry("url", Map.of("type", "url")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("properties", propertiesSchema));

        List<ModifiableValueProperty<?, ?>> properties = NotionUtils.createPropertiesForDatabaseItem(
            mockedParameters, null, null, mockedContext);

        List<ModifiableValueProperty<?, ?>> expectedProperties = List.of(
            bool("cb")
                .label("cb")
                .required(false),
            dateTime("dt")
                .label("dt")
                .required(false),
            string("email")
                .label("email")
                .required(false),
            string("select")
                .label("select")
                .options(option("option1", "option1"))
                .required(false),
            array("multi")
                .label("multi")
                .items(string())
                .options(List.of(option("option1", "option1")))
                .required(false),
            string("status")
                .label("status")
                .options(option("option1", "option1"))
                .required(false),
            number("number")
                .label("number")
                .required(false),
            string("phone")
                .label("phone")
                .required(false),
            string("rich")
                .label("rich")
                .controlType(TEXT_AREA)
                .required(false),
            string("title")
                .label("title")
                .required(false),
            string("url")
                .label("url")
                .required(false));

        assertTrue(expectedProperties.containsAll(properties));
        assertTrue(properties.containsAll(expectedProperties));

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/data_sources/xy", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetAllItems(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("results", List.of(Map.of(ID, "123")), "next_cursor", "nc"),
                Map.of("results", List.of(Map.of(ID, "345"))));

        List<Object> result = NotionUtils.getAllItems(mockedContext, "url", false, "filter", "x");

        assertEquals(List.of(Map.of(ID, "123"), Map.of(ID, "345")), result);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("url", "url"), stringArgumentCaptor.getAllValues());

        assertEquals(
            List.of(
                Http.Body.of(Map.of("filter", "x", "page_size", 100), Http.BodyContentType.JSON),
                Http.Body.of(
                    Map.of("filter", "x", "page_size", 100, "start_cursor", "nc"),
                    Http.BodyContentType.JSON)),
            bodyArgumentCaptor.getAllValues());
    }

    @Test
    void testGetDatabase(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        Map<String, ?> result = NotionUtils.getDatabase("xy", mockedContext);

        assertEquals(Map.of(), result);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/data_sources/xy", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetDatabaseIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Map<String, Object> db1 = Map.of(
            ID, "db1",
            TITLE, List.of(Map.of(
                "type", TEXT,
                TEXT, Map.of(CONTENT, "DB One"))));
        Map<String, Object> db2 = Map.of(
            ID, "db2",
            TITLE, List.of(Map.of(
                "type", TEXT,
                TEXT, Map.of(CONTENT, "DB Two"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("results", List.of(db1, db2)));

        List<Option<String>> result = NotionUtils.getDatabaseIdOptions(
            null, null, null, null, mockedContext);

        assertEquals(List.of(option("DB One", "db1"), option("DB Two", "db2")), result);

        Http.Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertEquals(Http.ResponseType.Type.JSON, configuration.getResponseType()
            .getType());
        assertEquals("/search", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(
                Map.of("filter", Map.of("property", "object", "value", "data_source"), "page_size", 100),
                Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testGetDatabasePropertyOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("properties", Map.of("p1", Map.of())));

        List<Option<String>> result = NotionUtils.getDatabasePropertyOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(List.of(option("p1", "p1")), result);

        Http.Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertEquals(Http.ResponseType.Type.JSON, configuration.getResponseType()
            .getType());
        assertEquals("/data_sources/xy", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetPageIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Map<String, Object> page1 = Map.of(
            ID, "p1",
            "properties", Map.of(
                TITLE, Map.of(
                    TITLE, List.of(Map.of(
                        "type", TEXT,
                        TEXT, Map.of(CONTENT, "Page One"))))));

        Map<String, Object> page2 = Map.of(
            ID, "p2",
            "properties", Map.of(
                TITLE, Map.of(
                    TITLE, List.of(Map.of(
                        "type", TEXT,
                        TEXT, Map.of(CONTENT, "Page Two"))))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("results", List.of(page1, page2)));

        List<Option<String>> result = NotionUtils.getPageIdOptions(
            null, null, null, null, mockedContext);

        assertEquals(List.of(option("Page One", "p1"), option("Page Two", "p2")), result);

        Http.Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertEquals(Http.ResponseType.Type.JSON, configuration.getResponseType()
            .getType());
        assertEquals("/search", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(
                Map.of("filter", Map.of("property", "object", "value", "page"), "page_size", 100),
                Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
