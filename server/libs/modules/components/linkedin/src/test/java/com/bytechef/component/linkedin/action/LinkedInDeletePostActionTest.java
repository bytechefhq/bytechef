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

package com.bytechef.component.linkedin.action;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.definition.Context.Encoder;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.BasePerformFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class LinkedInDeletePostActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Http.Executor>> encoderFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(URN, "123"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Encoder mockedEncoder = mock(Encoder.class);

    @Test
    void testPerform() throws Exception {
        Optional<? extends BasePerformFunction> basePerformFunction = LinkedInDeletePostAction.ACTION_DEFINITION
            .getPerform();

        assertTrue(basePerformFunction.isPresent());

        PerformFunction performFunction = (PerformFunction) basePerformFunction.get();

        when(mockedActionContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> encoderFunctionArgumentCaptor.getValue()
                .apply(mockedEncoder));
        when(mockedEncoder.base64UrlEncode(stringArgumentCaptor.capture()))
            .thenReturn("urn");

        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> httpFunctionArgumentCaptor.getValue()
                .apply(mockedHttp));
        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object result = performFunction.apply(mockedParameters, null, mockedActionContext);

        assertNull(result);
        assertEquals(List.of("123", "/rest/posts/urn"), stringArgumentCaptor.getAllValues());
    }
}
