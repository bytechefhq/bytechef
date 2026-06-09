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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Parameters;
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
class LinkedInDeletePostActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Executor>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(URN, "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Encoder mockedEncoder = mock(Encoder.class);

    @Test
    void testPerform(
        Context mockedActionContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        when(mockedActionContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> encoderFunctionArgumentCaptor.getValue()
                .apply(mockedEncoder));
        when(mockedEncoder.base64UrlEncode(stringArgumentCaptor.capture()))
            .thenReturn("urn");

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Object result = LinkedInDeletePostAction.perform(mockedParameters, null, mockedActionContext);

        assertNull(result);
        assertEquals(List.of("123", "/rest/posts/urn"), stringArgumentCaptor.getAllValues());
        assertNotNull(httpFunctionArgumentCaptor.getValue());
    }
}
