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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESTINATIONS;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGES;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDER;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class InfobipSendSMSActionTest extends AbstractInfobipActionTest {

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(SENDER))
            .thenReturn("123");
        when(mockedParameters.getRequiredList(TO, String.class))
            .thenReturn(List.of("abc", "def"));
        when(mockedParameters.getRequiredString(TEXT))
            .thenReturn("text");

        Map<String, Object> result = InfobipSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(MESSAGES, List.of(Map.of(SENDER, "123", DESTINATIONS, List.of(Map.of(TO, "abc"), Map.of(TO, "def")), CONTENT, Map.of(TEXT, "text")))), body.getContent());
    }
}
