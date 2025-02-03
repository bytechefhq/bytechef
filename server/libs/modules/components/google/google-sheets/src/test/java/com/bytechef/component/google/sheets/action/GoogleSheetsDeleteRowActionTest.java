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

import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

/**
 * @author Monika Kušter
 */
class GoogleSheetsDeleteRowActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ROW_NUMBER, 2));
    private final ArgumentCaptor<String> dimensionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> rowNumberArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void perform() throws Exception {
        try (MockedStatic<GoogleSheetsUtils> googleSheetsUtilsMockedStatic = mockStatic(GoogleSheetsUtils.class)) {

            googleSheetsUtilsMockedStatic
                .when(() -> GoogleSheetsUtils.deleteDimension(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                    rowNumberArgumentCaptor.capture(), dimensionArgumentCaptor.capture()))
                .thenAnswer((Answer<Void>) invocation -> null);

            Object result = GoogleSheetsDeleteRowAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertNull(result);

            assertEquals("ROWS", dimensionArgumentCaptor.getValue());
            assertEquals(2, rowNumberArgumentCaptor.getValue());
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
        }
    }
}
