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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.FIRST_NAME;
import static com.bytechef.component.brevo.constant.BrevoConstants.LAST_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class BrevoUpdateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(EMAIL, "test@test.com", FIRST_NAME, "test", LAST_NAME, "test"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Object result = BrevoUpdateContactAction.perform(mockedParameters, null, mockedContext);

        assertNull(result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/contacts/test@test.com", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of("attributes", Map.of(FIRST_NAME, "test", LAST_NAME, "test")), BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
