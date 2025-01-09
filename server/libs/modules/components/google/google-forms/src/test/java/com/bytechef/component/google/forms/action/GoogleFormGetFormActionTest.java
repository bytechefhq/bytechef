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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleFormGetFormActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<String> formIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FORM_ID, "formId"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Map<String, Object> map = new HashMap<>();

    @Test
    void testPerform() {
        try (MockedStatic<GoogleFormsUtils> googleFormsUtilsMockedStatic = mockStatic(GoogleFormsUtils.class)) {
            googleFormsUtilsMockedStatic
                .when(() -> GoogleFormsUtils.getForm(formIdArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(map);

            Map<String, Object> result =
                GoogleFormsGetFormAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(map, result);
            assertEquals("formId", formIdArgumentCaptor.getValue());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
        }
    }
}
