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

package com.bytechef.component.microsoft.excel.action;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUpdateWorksheetUtils;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
abstract class AbstractMicrosoftExcelActionTest {

    protected ArgumentCaptor<ActionContext> actionContextArgumentCaptor = ArgumentCaptor.forClass(ActionContext.class);
    protected ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    protected ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    protected MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic;
    protected MockedStatic<MicrosoftExcelUpdateWorksheetUtils> updateWorksheetUtilsMockedStatic;
    protected ActionContext mockedActionContext = mock(ActionContext.class);
    protected Http.Executor mockedExecutor = mock(Http.Executor.class);
    protected ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @BeforeEach
    void beforeEach() {
        microsoftExcelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class);
        updateWorksheetUtilsMockedStatic = mockStatic(MicrosoftExcelUpdateWorksheetUtils.class);
    }

    @AfterEach
    public void afterEach() {
        microsoftExcelUtilsMockedStatic.close();
        updateWorksheetUtilsMockedStatic.close();
    }
}
