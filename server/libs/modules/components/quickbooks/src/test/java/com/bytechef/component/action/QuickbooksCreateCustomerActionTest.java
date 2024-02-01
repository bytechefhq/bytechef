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

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.FAMILY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.GIVEN_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MIDDLE_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SUFFIX;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.quickbooks.action.QuickbooksCreateCustomerAction;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
public class QuickbooksCreateCustomerActionTest extends AbstractQuickbooksActionTest {

    @Disabled
    @Test
    public void testPerform() throws FMSException {
        when(mockedParameters.getRequiredString(DISPLAY_NAME))
            .thenReturn("DISPLAY_NAME");
        when(mockedParameters.getRequiredString(FAMILY_NAME))
            .thenReturn("FAMILY_NAME");
        when(mockedParameters.getRequiredString(GIVEN_NAME))
            .thenReturn("GIVEN_NAME");
        when(mockedParameters.getRequiredString(MIDDLE_NAME))
            .thenReturn("MIDDLE_NAME");
        when(mockedParameters.getRequiredString(SUFFIX))
            .thenReturn("SUFFIX");
        when(mockedParameters.getRequiredString(TITLE))
            .thenReturn("TITLE");

        QuickbooksCreateCustomerAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class));

        verify(mockedDataService, times(1))
            .add(entityArgumentCaptor.capture());

        Customer customer = (Customer) entityArgumentCaptor.getValue();

        assertEquals("DISPLAY_NAME", customer.getDisplayName());
        assertEquals("FAMILY_NAME", customer.getFamilyName());
        assertEquals("GIVEN_NAME", customer.getGivenName());
        assertEquals("MIDDLE_NAME", customer.getMiddleName());
        assertEquals("SUFFIX", customer.getSuffix());
        assertEquals("TITLE", customer.getTitle());
    }
}
