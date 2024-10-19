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

package com.bytechef.component.nutshell.action;

import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.LINKS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Kalaiyarasan Raja
 */
public class NutshellCreateLeadActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(DESCRIPTION, "Lead description", OWNER, "userId123"));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        // Mocking the HTTP request process
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = NutshellCreateLeadAction.perform(mockedParameters, mockedParameters, mockedContext);

        // Expected lead map
        Map<String, Object> leadMap = new HashMap<>();
        leadMap.put(DESCRIPTION, "Lead description");
        leadMap.put(LINKS, Map.of(OWNER, "userId123"));

        assertEquals(Map.of("leads", List.of(leadMap)), bodyArgumentCaptor.getValue()
            .getContent());
        assertEquals(mockedObject, result);

    }
}
