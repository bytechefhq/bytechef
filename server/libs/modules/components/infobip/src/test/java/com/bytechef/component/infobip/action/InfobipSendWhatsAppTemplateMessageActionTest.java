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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.LANGUAGE;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.PLACEHOLDERS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEMPLATE_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.infobip.util.InfobipUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class InfobipSendWhatsAppTemplateMessageActionTest extends AbstractInfobipActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);

    @Test
    void testPerform() {
        mockedParameters = MockParametersFactory.create(
            Map.of(FROM, "123", TO, "456", TEMPLATE_NAME, "template", PLACEHOLDERS,
                Map.of("_1", "value1", "_2", "value2", "_3", "value3")));

        try (MockedStatic<InfobipUtils> infobipUtilsMockedStatic = mockStatic(InfobipUtils.class)) {
            infobipUtilsMockedStatic
                .when(() -> InfobipUtils.getTemplates(stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(List.of(Map.of(NAME, "template", LANGUAGE, "en")));

            Map<String, Object> result = InfobipSendWhatsAppTemplateMessageAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);

            Http.Body body = bodyArgumentCaptor.getValue();

            Map<String, Object> expectedBody = Map.of(
                "messages", List.of(
                    Map.of(
                        FROM, "123",
                        TO, "456",
                        CONTENT, Map.of(
                            TEMPLATE_NAME, "template",
                            "templateData", Map.of("body", Map.of(PLACEHOLDERS, List.of("value1", "value2", "value3"))),
                            LANGUAGE, "en"))));

            assertEquals(expectedBody, body.getContent());
            assertEquals("123", stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
