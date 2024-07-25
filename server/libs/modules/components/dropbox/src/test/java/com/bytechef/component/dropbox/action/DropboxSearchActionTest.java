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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.dropbox.action.DropboxSearchAction.POST_SEARCH_CONTEXT_FUNCTION;
import static com.bytechef.component.dropbox.constant.DropboxConstants.QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class DropboxSearchActionTest extends AbstractDropboxActionTest {

    @Test
    void testPerform() {
        when(mockedParameters.getRequired(QUERY))
            .thenReturn("query");

        when(mockedContext.http(POST_SEARCH_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = DropboxSearchAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, String> expectedBody = Map.of(QUERY, "query");

        assertEquals(expectedBody, body.getContent());
    }
}
