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

package com.bytechef.component.intercom.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IntercomUtilsTest {
    private final ActionContext mockedContext = Mockito.mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = Mockito.mock(Context.Http.Executor.class);
    private final Context.Http.Response mockedResponse = Mockito.mock(Context.Http.Response.class);

    @BeforeEach
    public void beforeEach() {

        Mockito.when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetContactRole() {
        String id = "exampleId";
        String role = "exampleRole";
        Map<String, Object> body = new HashMap<>();
        body.put("role", role);

        Mockito.when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        Map<String, String> result = IntercomUtils.getContactRole(id, mockedContext);

        assertEquals(2, result.size());
        assertEquals(role, result.get("type"));
        assertEquals(id, result.get("id"));
    }

    @Test
    void testGetAdminId() {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> adminData = new HashMap<>();

        String id = "exampleId";
        adminData.put("id", id);
        body.put("admins", List.of(adminData));

        Mockito.when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        Map<String, String> result = IntercomUtils.getAdminId(mockedContext);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("admin", result.get("type"));
        assertEquals(id, result.get("id"));

    }

}
