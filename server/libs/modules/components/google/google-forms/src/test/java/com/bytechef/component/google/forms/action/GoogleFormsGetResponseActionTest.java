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

package com.bytechef.component.google.forms.action;

import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Vihar Shah
 * @author Monika Kušter
 */
class GoogleFormsGetResponseActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<String> formIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FORM_ID, "formId", RESPONSE_ID, "responseId"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Map> responseArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final Map<String, Object> map = new HashMap<>();

    @Test
    void testPerform() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        try (MockedStatic<GoogleFormsUtils> googleFormsUtilsMockedStatic = mockStatic(GoogleFormsUtils.class)) {
            googleFormsUtilsMockedStatic
                .when(() -> GoogleFormsUtils.createCustomResponse(contextArgumentCaptor.capture(),
                    formIdArgumentCaptor.capture(), responseArgumentCaptor.capture()))
                .thenReturn(map);

            Map<String, Object> result =
                GoogleFormsGetResponseAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(map, result);
            assertEquals("formId", formIdArgumentCaptor.getValue());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            assertEquals(map, responseArgumentCaptor.getValue());
        }
    }
}
