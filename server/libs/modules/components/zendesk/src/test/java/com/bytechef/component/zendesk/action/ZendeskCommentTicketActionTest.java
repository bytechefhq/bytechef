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

package com.bytechef.component.zendesk.action;

import static com.bytechef.component.zendesk.constant.ZendeskConstants.BODY;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.COMMENT;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TICKET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class ZendeskCommentTicketActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(COMMENT, "comment"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of(TICKET, Map.of());

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = ZendeskCommentTicketAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);

        Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expectedBody = Map.of(TICKET, Map.of(COMMENT, Map.of(BODY, "comment")));

        assertEquals(expectedBody, body.getContent());
    }
}
