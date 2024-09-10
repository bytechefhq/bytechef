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

package com.bytechef.component.whatsapp.action;

import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.BODY;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.MESSAGING_PRODUCT;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.RECEIVE_USER;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.RECIPIENT_TYPE;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.TEXT;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubić
 */
class WhatsAppSendMessageActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");

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

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(MESSAGING_PRODUCT))
            .thenReturn((String) propertyStubsMap.get(MESSAGING_PRODUCT));
        when(mockedParameters.getString(RECIPIENT_TYPE))
            .thenReturn((String) propertyStubsMap.get(RECIPIENT_TYPE));
        when(mockedParameters.getString(RECEIVE_USER))
            .thenReturn((String) propertyStubsMap.get(RECEIVE_USER));
        when(mockedParameters.getString(TYPE))
            .thenReturn((String) propertyStubsMap.get(TYPE));
        when(mockedParameters.getRequired(TEXT))
            .thenReturn(propertyStubsMap.get(TEXT));
        when(mockedParameters.getRequiredString(BODY))
            .thenReturn("Some Message");

        Object result = WhatsAppSendMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new LinkedHashMap<>();
        Map<String, Object> bodyText = new LinkedHashMap<>();

        bodyText.put(BODY, "Some Message");

        propertyStubsMap.put(MESSAGING_PRODUCT, "whatsapp");
        propertyStubsMap.put(RECIPIENT_TYPE, "individual");
        propertyStubsMap.put(TYPE, "text");
        propertyStubsMap.put(TEXT, bodyText);

        return propertyStubsMap;
    }
}
