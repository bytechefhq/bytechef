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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Parameters;
import com.infobip.ApiClient;
import com.infobip.ApiKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
public abstract class AbstractInfobipActionTest {

    protected ArgumentCaptor<ApiKey> apiKeyArgumentCaptor = ArgumentCaptor.forClass(ApiKey.class);
    protected ApiClient mockedApiClient = mock(ApiClient.class);
    protected ApiClient.Builder mockedBuilder = mock(ApiClient.Builder.class);
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected MockedStatic<ApiClient> apiClientMockedStatic;

    @BeforeEach
    public void beforeEach() {
        apiClientMockedStatic = mockStatic(ApiClient.class);

        apiClientMockedStatic.when(
            () -> ApiClient.forApiKey(apiKeyArgumentCaptor.capture()))
            .thenReturn(mockedBuilder);

        when(mockedBuilder.build()).thenReturn(mockedApiClient);
    }

    @AfterEach
    public void afterEach() {
        apiClientMockedStatic.close();
    }
}
