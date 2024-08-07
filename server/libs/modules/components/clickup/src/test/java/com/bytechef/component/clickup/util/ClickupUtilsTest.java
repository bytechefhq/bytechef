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

package com.bytechef.component.clickup.util;

import static com.bytechef.component.clickup.constant.ClickupConstants.FOLDER_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.NAME;
import static com.bytechef.component.clickup.constant.ClickupConstants.SPACE_ID;
import static com.bytechef.component.definition.ComponentDSL.option;
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
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * * @author Monika Kušter
 */
class ClickupUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final List<Option<String>> expectedOptions = List.of(option("some name", "abc"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected WebhookBody mockedWebhookBody = mock(WebhookBody.class);

    @BeforeEach()
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetAllListIdOptions() {
        when(mockedParameters.getString(FOLDER_ID))
            .thenReturn("folder");
        when(mockedParameters.getRequiredString(SPACE_ID))
            .thenReturn("space");

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("lists", List.of(Map.of(NAME, "some name", ID, "abc"))));

        assertEquals(List.of(option("some name", "abc"), option("some name", "abc")),
            ClickupUtils.getAllListIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetFolderIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("folders", List.of(Map.of(NAME, "some name", ID, "abc"))));

        assertEquals(expectedOptions,
            ClickupUtils.getFolderIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetSpaceIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("spaces", List.of(Map.of(NAME, "some name", ID, "abc"))));

        assertEquals(expectedOptions,
            ClickupUtils.getSpaceIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetWorkspaceIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("teams", List.of(Map.of(NAME, "some name", ID, "abc"))));

        assertEquals(expectedOptions,
            ClickupUtils.getWorkspaceIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testSubscribeWebhook() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "123"));

       String id = ClickupUtils.subscribeWebhook("webhookUrl", mockedTriggerContext, "id", "eventType");

        assertEquals("123", id);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("endpoint", "webhookUrl", "events", List.of("eventType")), body.getContent());
    }

    @Test
    void testUnsubscribeWebhook() {
        ClickupUtils.unsubscribeWebhook(mockedTriggerContext, "");

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }

    @Test
    void testGetCreatedObject() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "123"));

        assertEquals(Map.of(ID, "123"), ClickupUtils.getCreatedObject(mockedWebhookBody, mockedTriggerContext, "id", "path"));
    }
}
