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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.sendgrid.constant.SendgridConstants.CUSTOM_UNSUBSCRIBE_URL;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.HTML_CONTENT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.LIST_IDS;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.NAME;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SENDER_ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUBJECT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUPPRESSION_GROUP_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class SendgridCreateSingleSendActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Map<String, Object> responseMap = Map.of("id", "single-send-1", "status", "draft");

    @Test
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp, Response mockedResponse,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                NAME, "Spring Sale", SUBJECT, "Our spring sale starts now", HTML_CONTENT, "<p>Hello</p>",
                SENDER_ID, 12, LIST_IDS, List.of("list-1", "list-2"), SUPPRESSION_GROUP_ID, 34));

        mockHttp(mockedExecutor, mockedHttp, mockedResponse);

        Map<String, Object> result = SendgridCreateSingleSendAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);
        assertEquals("/marketing/singlesends", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(Http.ResponseType.JSON, configurationBuilder.build()
            .getResponseType());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(
                "name", "Spring Sale",
                "send_to", Map.of("list_ids", List.of("list-1", "list-2")),
                "email_config", Map.of(
                    "subject", "Our spring sale starts now",
                    "html_content", "<p>Hello</p>",
                    "sender_id", 12L,
                    "suppression_group_id", 34L)),
            body.getContent());
    }

    @Test
    void testPerformWithCustomUnsubscribeUrl(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp, Response mockedResponse) {

        Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(NAME, "Spring Sale");
        parameterMap.put(SUBJECT, "Our spring sale starts now");
        parameterMap.put(HTML_CONTENT, "<p>Hello</p>");
        parameterMap.put(SENDER_ID, 12);
        parameterMap.put(LIST_IDS, List.of("list-1"));
        parameterMap.put(CUSTOM_UNSUBSCRIBE_URL, "https://example.com/unsubscribe");

        Parameters mockedParameters = MockParametersFactory.create(parameterMap);

        mockHttp(mockedExecutor, mockedHttp, mockedResponse);

        SendgridCreateSingleSendAction.perform(mockedParameters, null, mockedContext);

        Body body = bodyArgumentCaptor.getValue();

        Map<?, ?> content = (Map<?, ?>) body.getContent();

        assertEquals(
            Map.of(
                "subject", "Our spring sale starts now",
                "html_content", "<p>Hello</p>",
                "sender_id", 12L,
                "custom_unsubscribe_url", "https://example.com/unsubscribe"),
            content.get("email_config"));
    }

    private void mockHttp(Executor mockedExecutor, Http mockedHttp, Response mockedResponse) {
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);
    }
}
