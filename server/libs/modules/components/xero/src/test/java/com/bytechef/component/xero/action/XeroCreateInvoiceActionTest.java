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

package com.bytechef.component.xero.action;

import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public class XeroCreateInvoiceActionTest {

    @Test
    public void testPerform() {
        ActionContext mockedContext = Mockito.mock(ActionContext.class);
        Context.Http.Executor mockedExecutor = Mockito.mock(Context.Http.Executor.class);
        Context.Http.Response mockedResponse = Mockito.mock(Context.Http.Response.class);

        LinkedHashMap responseMap = new LinkedHashMap();
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(TYPE, "typePropertyStub");
        propertyStubsMap.put("Contact", Map.of(CONTACT_ID, "contactIdPropertyStub"));
        propertyStubsMap.put(LINE_ITEMS, new LinkedList<>()); // todo: test individual Line item properties

        Parameters mockedParameters = Mockito.mock(Parameters.class);

        when(mockedParameters.getRequiredString(TYPE))
            .thenReturn(propertyStubsMap.get(TYPE)
                .toString());
        when(mockedParameters.getRequiredString(CONTACT_ID))
            .thenReturn("contactIdPropertyStub");

        ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Context.Http.Body.class);
        ArgumentCaptor<Context.ContextFunction> contextFunctionArgumentCaptor =
            ArgumentCaptor.forClass(Context.ContextFunction.class);

        when(mockedContext.http(contextFunctionArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(Mockito.any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody((Context.TypeReference<Object>) Mockito.any()))
            .thenReturn(responseMap);

        LinkedHashMap<String, ?> result =
            XeroCreateInvoiceAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNotNull(result);
        assertEquals(propertyStubsMap, bodyArgumentCaptor.getValue()
            .getContent());

        // todo: somehow validate lambda parameter of http method
//        Context.ContextFunction<Context.Http, ?> expectedFunction =
//            http -> http.post("https://api.xero.com/api.xro/2.0/Invoices");
//        assertEquals(expectedFunction, functionArgumentCaptor.getValue());
    }
}
