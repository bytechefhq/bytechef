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

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ASSET_ACCOUNT_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.EXPENSE_ACCOUNT_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INCOME_ACCOUNT_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INVENTORY_START_DATE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ITEM_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QUANTITY_ON_HAND;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.quickbooks.action.QuickbooksCreateItemAction;
import com.intuit.ipp.data.Item;
import com.intuit.ipp.data.ItemTypeEnum;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.exception.FMSException;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
public class QuickbooksCreateItemActionTest extends AbstractQuickbooksActionTest {

    @Test
    public void testPerform() throws FMSException {
        when(mockedParameters.getRequiredString(ASSET_ACCOUNT_ID))
            .thenReturn("ASSET_ACCOUNT_ID");
        when(mockedParameters.getRequiredString(EXPENSE_ACCOUNT_ID))
            .thenReturn("EXPENSE_ACCOUNT_ID");
        when(mockedParameters.getRequiredString(INCOME_ACCOUNT_ID))
            .thenReturn("INCOME_ACCOUNT_ID");

        Date invenoryStartDate = new Date();

        when(mockedParameters.getDate(INVENTORY_START_DATE))
            .thenReturn(invenoryStartDate);
        when(mockedParameters.getRequiredString(ITEM_NAME))
            .thenReturn("ITEM_NAME");
        when(mockedParameters.getRequiredInteger(QUANTITY_ON_HAND))
            .thenReturn(11);
        when(mockedParameters.getRequiredString(TYPE))
            .thenReturn("inventory");

        QuickbooksCreateItemAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class));

        verify(mockedDataService, times(1))
            .add(entityArgumentCaptor.capture());

        Item item = (Item) entityArgumentCaptor.getValue();

        ReferenceType referenceType = item.getAssetAccountRef();

        assertEquals("ASSET_ACCOUNT_ID", referenceType.getValue());

        referenceType = item.getExpenseAccountRef();

        assertEquals("EXPENSE_ACCOUNT_ID", referenceType.getValue());

        referenceType = item.getIncomeAccountRef();

        assertEquals("INCOME_ACCOUNT_ID", referenceType.getValue());

        assertEquals(invenoryStartDate, item.getInvStartDate());
        assertEquals("ITEM_NAME", item.getName());
        assertEquals(new BigDecimal(11), item.getQtyOnHand());
        assertEquals(true, item.isTrackQtyOnHand());
        assertEquals(ItemTypeEnum.INVENTORY, item.getType());
    }
}
