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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REPLY_TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class MicrosoftOutlook365SendEmailActionTest extends AbstractMicrosoftOutlook365ActionTest {

    private final ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Context.Http.Body.class);

    @Test
    void testPerform() {
        Map<String, String> responeseMap = Map.of("key", "value");
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.get(FROM))
            .thenReturn(propertyStubsMap.get(FROM));
        when(mockedParameters.getRequiredString(SUBJECT))
            .thenReturn((String) propertyStubsMap.get(SUBJECT));
        when(mockedParameters.get(BODY))
            .thenReturn(propertyStubsMap.get(BODY));
        when(mockedParameters.getArray(TO_RECIPIENTS))
            .thenReturn((Object[]) propertyStubsMap.get(TO_RECIPIENTS));
        when(mockedParameters.getArray(CC_RECIPIENTS))
            .thenReturn((Object[]) propertyStubsMap.get(CC_RECIPIENTS));
        when(mockedParameters.getArray(BCC_RECIPIENTS))
            .thenReturn((Object[]) propertyStubsMap.get(BCC_RECIPIENTS));
        when(mockedParameters.getArray(REPLY_TO))
            .thenReturn((Object[]) propertyStubsMap.get(REPLY_TO));

        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);

        Object result = MicrosoftOutlook365SendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNull(result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("message", propertyStubsMap), body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, String> content = Map.of("content", "test", "contentType", "text");
        Map<String, Map<String, String>> recipient = Map.of(
            "emailAddress", Map.of("address", "address", "name", "name"));
        Object[] array = List.of(recipient)
            .toArray();

        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(FROM, "testFrom");
        propertyStubsMap.put(SUBJECT, "testSubject");
        propertyStubsMap.put(BODY, content);
        propertyStubsMap.put(TO_RECIPIENTS, array);
        propertyStubsMap.put(CC_RECIPIENTS, array);
        propertyStubsMap.put(BCC_RECIPIENTS, array);
        propertyStubsMap.put(REPLY_TO, array);

        return propertyStubsMap;
    }
}
