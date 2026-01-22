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

package com.bytechef.component.google.workspace.admin.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.EMAIL;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.MAX_RESULTS;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.PAGE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class GoogleWorkspaceAdminUtilsTest {
    private final ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void getRoleIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "items", List.of(
                        Map.of("roleName", "Name 1", "roleId", "Role 1"),
                        Map.of("roleName", "Name 2", "roleId", "Role 2")),
                    NEXT_PAGE_TOKEN, "t1"))
            .thenReturn(Map.of("items", List.of(Map.of("roleName", "Name 3", "roleId", "Role 3"))));

        List<Option<String>> result = GoogleWorkspaceAdminUtils.getRoleIdOptions(
            mockedParameters, null, null, null, mockedContext);

        List<Option<String>> expected = List.of(
            option("Name 1", "Role 1"),
            option("Name 2", "Role 2"),
            option("Name 3", "Role 3"));

        assertEquals(expected, result);

        performCommonAssertions(
            "https://admin.googleapis.com/admin/directory/v1/customer/my_customer/roles");

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            PAGE_TOKEN, null, MAX_RESULTS, 100
        };
        Object[] queryParameters2 = {
            PAGE_TOKEN, "t1", MAX_RESULTS, 100
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }

    @Test
    void getUserIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "users", List.of(
                    Map.of(EMAIL, "email1@gmail.com", "id", "1"),
                    Map.of(EMAIL, "email2@gmail.com", "id", "2")),
                NEXT_PAGE_TOKEN, "t1"))
            .thenReturn(Map.of("users", List.of(Map.of(EMAIL, "email3@gmail.com", "id", "3"))));

        List<Option<String>> result = GoogleWorkspaceAdminUtils.getUserIdOptions(
            mockedParameters, null, null, null, mockedContext);

        List<Option<String>> expected = List.of(
            option("email1@gmail.com", "1"),
            option("email2@gmail.com", "2"),
            option("email3@gmail.com", "3"));

        assertEquals(expected, result);

        performCommonAssertions(
            "https://admin.googleapis.com/admin/directory/v1/users");

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            "customer", "my_customer", PAGE_TOKEN, null, MAX_RESULTS, 100
        };
        Object[] queryParameters2 = {
            "customer", "my_customer", PAGE_TOKEN, "t1", MAX_RESULTS, 100
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }

    private void performCommonAssertions(String expectedUrl) {
        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals(expectedUrl, stringArgumentCaptor.getValue());
    }
}
