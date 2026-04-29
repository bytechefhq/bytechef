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

package com.bytechef.component.loops.action;

import static com.bytechef.component.loops.constant.LoopsConstants.EMAIL;
import static com.bytechef.component.loops.constant.LoopsConstants.FIRST_NAME;
import static com.bytechef.component.loops.constant.LoopsConstants.LAST_NAME;
import static com.bytechef.component.loops.constant.LoopsConstants.MAILING_LISTS;
import static com.bytechef.component.loops.constant.LoopsConstants.USER_GROUP;
import static com.bytechef.component.loops.constant.LoopsConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class LoopsCreateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(EMAIL, "email", FIRST_NAME, "name", LAST_NAME, "name", USER_GROUP, "userGroup",
            USER_ID, "userId", MAILING_LISTS, List.of("mailingList1", "mailingList2")));
    private final Map<String, Object> responseMap = Map.of("success", true, "id", "contactId");
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Object result = LoopsCreateContactAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/contacts/create", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of(EMAIL, "email", FIRST_NAME, "name", LAST_NAME, "name", USER_GROUP, "userGroup",
                USER_ID, "userId", MAILING_LISTS, Map.of("mailingList1", true, "mailingList2", true)),
            body.getContent());
    }
}
