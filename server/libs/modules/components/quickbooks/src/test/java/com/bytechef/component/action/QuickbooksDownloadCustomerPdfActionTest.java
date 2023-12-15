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

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER_ID;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bytechef.component.quickbooks.action.QuickbooksDownloadCustomerPdfAction;
import com.bytechef.hermes.component.definition.ActionContext;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public class QuickbooksDownloadCustomerPdfActionTest extends AbstractQuickbooksActionTest {

    @Test
    public void testPerform() throws FMSException {
        Mockito
            .when(parameters.getRequiredString(CUSTOMER_ID))
            .thenReturn(ID_STUB);

        QuickbooksDownloadCustomerPdfAction.perform(parameters, parameters, Mockito.mock(ActionContext.class));

        then(dataService)
            .should(times(1))
            .downloadPDF(entityArgumentCaptor.capture());

        verifyNoMoreInteractions(dataService);

        Assertions.assertEquals(ID_STUB, ((Customer) entityArgumentCaptor.getValue()).getId());
        Assertions.assertInstanceOf(Customer.class, entityArgumentCaptor.getValue());

    }
}
