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

package com.bytechef.component.action;

import static com.bytechef.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.services.DataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public abstract class AbstractQuickbooksActionTest {

    protected DataService mockedDataService;
    protected ArgumentCaptor<IEntity> entityArgumentCaptor;
    protected Parameters mockedParameters;

    private MockedStatic<QuickbooksUtils> mockedQuickbooksUtils;

    @BeforeEach
    public void beforeEach() {
        mockedDataService = mock(DataService.class);
        entityArgumentCaptor = ArgumentCaptor.forClass(IEntity.class);
        mockedParameters = mock(Parameters.class);
        mockedQuickbooksUtils = mockStatic(QuickbooksUtils.class);

        Mockito
            .when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("");

        mockedQuickbooksUtils
            .when(() -> QuickbooksUtils.getDataService(any()))
            .thenReturn(mockedDataService);
    }

    @AfterEach
    public void afterEach() {
        mockedQuickbooksUtils.close();
    }
}
