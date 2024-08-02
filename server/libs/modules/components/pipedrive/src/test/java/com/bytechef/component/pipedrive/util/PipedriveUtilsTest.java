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

package com.bytechef.component.pipedrive.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class PipedriveUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<Map<String, List<String>>> queryArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);

    @Test
    void testGetOptions() throws Exception {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", List.of(Map.of(ID, 123, "name", "ime"))));

        List<Option<String>> expectedOptions = List.of(option("ime", "123"));

        List<? extends Option<String>> result = PipedriveUtils.getOptions("org", null).apply(mockedParameters, mockedParameters, null, "", mockedActionContext);

        assertEquals(expectedOptions, result);

        assertEquals(Map.of(), queryArgumentCaptor.getValue());
    }

    @Test
    void testSubscribeWebhok() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", Map.of(ID, 123)));

        assertEquals(123,
            PipedriveUtils.subscribeWebhook("eventObject", "eventAction", "webhookUrl", mockedTriggerContext));

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, String> expectedMap = Map.of(
            "event_object", "eventObject",
            "event_action", "eventAction",
            "subscription_url", "webhookUrl");

        assertEquals(expectedMap, body.getContent());
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        PipedriveUtils.unsubscribeWebhook(123, mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
