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

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bytechef.component.quickbooks.action.QuickbooksCreateItemAction;
import com.bytechef.hermes.component.definition.ActionContext;
import com.intuit.ipp.data.Item;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.QueryResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public class QuickbooksCreateItemActionTest extends AbstractQuickbooksActionTest {
    static final String QUERY_STUB = "select * from Account";

    @Test
    public void testPerform() throws FMSException {
        Mockito
            .when(parameters.getRequiredString(TYPE))
            .thenReturn("inventory");

        QuickbooksCreateItemAction.perform(parameters, parameters, Mockito.mock(ActionContext.class));

        then(dataService)
            .should(times(1))
            .add(entityArgumentCaptor.capture());

        verifyNoMoreInteractions(dataService);

        Assertions.assertInstanceOf(Item.class, entityArgumentCaptor.getValue(),
            "Created entity must be of type Item!");
    }

    @Test
    @SuppressFBWarnings
    public void testFetchAllAccounts() throws FMSException {
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        QueryResult queryResult = Mockito.mock(QueryResult.class);

        Mockito
            .when(dataService.executeQuery(QUERY_STUB))
            .thenReturn(queryResult);

        QuickbooksCreateItemAction.fetchAllAccounts(parameters);

        then(dataService)
            .should(times(1))
            .executeQuery(stringArgumentCaptor.capture());

        then(queryResult)
            .should(times(1))
            .getEntities();

        Assertions.assertEquals(QUERY_STUB, stringArgumentCaptor.getValue());
    }

}
