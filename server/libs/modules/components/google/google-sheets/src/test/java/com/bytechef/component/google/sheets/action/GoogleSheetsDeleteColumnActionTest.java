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

package com.bytechef.component.google.sheets.action;

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.LABEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class GoogleSheetsDeleteColumnActionTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(LABEL, "B"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void perform() throws Exception {
        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {
            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.deleteDimension(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                    integerArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            Object result = GoogleSheetsDeleteColumnAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertNull(result);

            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(2, integerArgumentCaptor.getValue());
            assertEquals("COLUMNS", stringArgumentCaptor.getValue());
        }
    }
}
