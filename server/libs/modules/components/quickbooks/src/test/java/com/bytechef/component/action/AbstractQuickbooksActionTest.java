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

import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.bytechef.hermes.component.definition.Parameters;
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
    protected static final String ID_STUB = "idStub";
    protected MockedStatic<QuickbooksUtils> quickbooksUtils;
    protected DataService dataService;
    protected Parameters parameters;
    protected ArgumentCaptor<IEntity> entityArgumentCaptor;

    @BeforeEach
    public void beforeEach() {
        entityArgumentCaptor = ArgumentCaptor.forClass(IEntity.class);

        quickbooksUtils = Mockito.mockStatic(QuickbooksUtils.class);
        dataService = Mockito.mock(DataService.class);
        parameters = Mockito.mock(Parameters.class);

        Mockito
            .when(parameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("");

        quickbooksUtils
            .when(() -> QuickbooksUtils.getDataService(""))
            .thenReturn(dataService);
    }

    @AfterEach
    public void afterEach() {
        quickbooksUtils.close();
    }
}
