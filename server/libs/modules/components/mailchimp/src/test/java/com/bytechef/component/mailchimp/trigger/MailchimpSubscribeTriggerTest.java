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

package com.bytechef.component.mailchimp.trigger;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.mailchimp.trigger.MailchimpSubscribeTrigger.LIST_ID;
import static com.bytechef.component.mailchimp.trigger.MailchimpSubscribeTrigger.SUBSCRIBE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mailchimp.util.MailchimpUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MailchimpSubscribeTriggerTest {

    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(LIST_ID, "xy"));
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "at"));
    private final Parameters mockedOutputParameters = MockParametersFactory.create(Map.of("id", "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);

    @Test
    void testWebhookEnable(
        Http mockedHttp, Executor mockedExecutor, TriggerContext mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<MailchimpUtils> mailchimpUtilsMockedStatic = mockStatic(MailchimpUtils.class)) {
            mailchimpUtilsMockedStatic.when(
                () -> MailchimpUtils.getMailChimpServer(
                    stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
                .thenReturn("server");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of("id", "123"));

            WebhookEnableOutput result = MailchimpSubscribeTrigger.webhookEnable(
                mockedInputParameters, mockedConnectionParameters, "webhookUrl", "", mockedContext);

            assertEquals(new WebhookEnableOutput(Map.of("id", "123"), null), result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals(
                List.of("at", "https://server.api.mailchimp.com/3.0/lists/xy/webhooks"),
                stringArgumentCaptor.getAllValues());
            assertEquals(
                Body.of("url", "webhookUrl", "events", Map.of(SUBSCRIBE, true), "sources",
                    Map.of("user", true, "admin", true, "api", true)),
                bodyArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals(mockedContext, triggerContextArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable(
        Http mockedHttp, Executor mockedExecutor, TriggerContext mockedContext,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        try (MockedStatic<MailchimpUtils> mailchimpUtilsMockedStatic = mockStatic(MailchimpUtils.class)) {
            mailchimpUtilsMockedStatic.when(
                () -> MailchimpUtils.getMailChimpServer(
                    stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
                .thenReturn("server");

            when(mockedHttp.delete(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);

            MailchimpSubscribeTrigger.webhookDisable(
                mockedInputParameters, mockedConnectionParameters, mockedOutputParameters, "", mockedContext);

            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals(
                List.of("at", "https://server.api.mailchimp.com/3.0/lists/xy/webhooks/123"),
                stringArgumentCaptor.getAllValues());
            assertEquals(mockedContext, triggerContextArgumentCaptor.getValue());
        }
    }
}
