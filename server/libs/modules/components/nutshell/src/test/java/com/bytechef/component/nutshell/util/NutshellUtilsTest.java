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

package com.bytechef.component.nutshell.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAIL;
import static com.bytechef.component.nutshell.constant.NutshellConstants.EMAILS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.ID;
import static com.bytechef.component.nutshell.constant.NutshellConstants.LINKS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.NAME;
import static com.bytechef.component.nutshell.constant.NutshellConstants.OWNER;
import static com.bytechef.component.nutshell.constant.NutshellConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class NutshellUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testCreateEntityBasedOnType() {

        Parameters mockedEntityParameters = MockParametersFactory.create(
            Map.of(NAME, "full name", DESCRIPTION, "some description", EMAIL, "test@mail.com"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = NutshellUtils.createEntityBasedOnType(mockedEntityParameters, mockedContext, false);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> contactMap = new HashMap<>();

        contactMap.put(NAME, "full name");
        contactMap.put(DESCRIPTION, "some description");
        contactMap.put(EMAILS, List.of(Map.of(VALUE, "test@mail.com")));

        assertEquals(Map.of("contacts", List.of(contactMap)), body.getContent());

        assertEquals(mockedObject, result);
    }

    @Test
    void testAddIfPresent() {

        Map<String, Object> inputParams = Map.of(OWNER, "1-testuser");
        Parameters mockedParams = MockParametersFactory.create(inputParams);

        Map<String, Object> requestMap = new HashMap<>();
        NutshellUtils.addIfPresent(mockedParams, OWNER, LINKS, requestMap);

        assertEquals(Map.of(LINKS, inputParams), requestMap);
    }

    @Test
    void testGetUserOptions() {

        // User Response
        Map<String, Object> users = new LinkedHashMap<>();
        users.put(NAME, "Test User");
        users.put(ID, "12345");
        List<Map<String, Object>> usersList = List.of(users);
        Map<String, Object> responseBody = Map.of("users", usersList);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseBody);

        List<Option<String>> expectedOptions = new ArrayList<>();
        expectedOptions.add(option("Test User", "12345"));

        assertEquals(expectedOptions,
            NutshellUtils.getUserOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

    }
}
