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

import static com.bytechef.component.intercom.constant.IntercomConstants.ID;
import static com.bytechef.component.intercom.constant.IntercomConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntercomUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    public void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetContactRole() {
        String id = "exampleId";
        String role = "exampleRole";
        Map<String, Object> body = new HashMap<>();

        body.put("role", role);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        Map<String, String> result = IntercomUtils.getContactRole(id, mockedContext);

        assertEquals(2, result.size());
        assertEquals(role, result.get(TYPE));
        assertEquals(id, result.get(ID));
    }

    @Test
    void testGetAdminId() {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> adminData = new HashMap<>();

        String id = "exampleId";

        adminData.put("id", id);
        body.put("admins", List.of(adminData));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        Map<String, String> result = IntercomUtils.getAdminId(mockedContext);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("admin", result.get(TYPE));
        assertEquals(id, result.get(ID));
    }
}
