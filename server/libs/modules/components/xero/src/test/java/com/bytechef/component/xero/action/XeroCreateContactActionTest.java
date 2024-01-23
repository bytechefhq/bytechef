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

import static com.bytechef.component.xero.constant.XeroConstants.BANK_ACCOUNT_DETAILS;
import static com.bytechef.component.xero.constant.XeroConstants.COMPANY_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.EMAIL_ADDRESS;
import static com.bytechef.component.xero.constant.XeroConstants.FIRST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.IS_CUSTOMER;
import static com.bytechef.component.xero.constant.XeroConstants.IS_SUPPLIER;
import static com.bytechef.component.xero.constant.XeroConstants.LAST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.TAX_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.TRACKING_CATEGORY_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.TRACKING_OPTION_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
public class XeroCreateContactActionTest {

    @Test
    public void testPerform() {
        ActionContext mockedContext = Mockito.mock(ActionContext.class);
        Context.Http.Executor mockedExecutor = Mockito.mock(Context.Http.Executor.class);
        Context.Http.Response mockedResponse = Mockito.mock(Context.Http.Response.class);

        LinkedHashMap responseMap = new LinkedHashMap();
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NAME, "namePropertyStub");
        propertyStubsMap.put(CONTACT_NUMBER, "contactNumberPropertyStub");
        propertyStubsMap.put(FIRST_NAME, "firstNamePropertyStub");
        propertyStubsMap.put(LAST_NAME, "lastNamePropertyStub");
        propertyStubsMap.put(COMPANY_NUMBER, "companyNumberPropertyStub");
        propertyStubsMap.put(EMAIL_ADDRESS, "emailAddressPropertyStub");
        propertyStubsMap.put(BANK_ACCOUNT_DETAILS, "bankAccountDetailsPropertyStub");
        propertyStubsMap.put(TAX_NUMBER, "taxNumberPropertyStub");
        propertyStubsMap.put(IS_SUPPLIER, true);
        propertyStubsMap.put(IS_CUSTOMER, true);
        propertyStubsMap.put(TRACKING_CATEGORY_NAME, "trackingCategoryPropertyStub");
        propertyStubsMap.put(TRACKING_OPTION_NAME, "trackingOptionPropertyStub");

        Parameters mockedParameters = Mockito.mock(Parameters.class);

        when(mockedParameters.getRequiredString(NAME))
            .thenReturn(propertyStubsMap.get(NAME)
                .toString());
        when(mockedParameters.getString(CONTACT_NUMBER))
            .thenReturn(propertyStubsMap.get(CONTACT_NUMBER)
                .toString());
        when(mockedParameters.getString(FIRST_NAME))
            .thenReturn(propertyStubsMap.get(FIRST_NAME)
                .toString());
        when(mockedParameters.getString(LAST_NAME))
            .thenReturn(propertyStubsMap.get(LAST_NAME)
                .toString());
        when(mockedParameters.getString(COMPANY_NUMBER))
            .thenReturn(propertyStubsMap.get(COMPANY_NUMBER)
                .toString());
        when(mockedParameters.getString(EMAIL_ADDRESS))
            .thenReturn(propertyStubsMap.get(EMAIL_ADDRESS)
                .toString());
        when(mockedParameters.getString(BANK_ACCOUNT_DETAILS))
            .thenReturn(propertyStubsMap.get(BANK_ACCOUNT_DETAILS)
                .toString());
        when(mockedParameters.getString(TAX_NUMBER))
            .thenReturn(propertyStubsMap.get(TAX_NUMBER)
                .toString());
        when(mockedParameters.getBoolean(IS_SUPPLIER))
            .thenReturn((Boolean) propertyStubsMap.get(IS_SUPPLIER));
        when(mockedParameters.getBoolean(IS_CUSTOMER))
            .thenReturn((Boolean) propertyStubsMap.get(IS_CUSTOMER));
        when(mockedParameters.getString(TRACKING_CATEGORY_NAME))
            .thenReturn(propertyStubsMap.get(TRACKING_CATEGORY_NAME)
                .toString());
        when(mockedParameters.getString(TRACKING_OPTION_NAME))
            .thenReturn(propertyStubsMap.get(TRACKING_OPTION_NAME)
                .toString());

        ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Context.Http.Body.class);
        ArgumentCaptor<Context.ContextFunction> functionArgumentCaptor =
            ArgumentCaptor.forClass(Context.ContextFunction.class);

        when(mockedContext.http(functionArgumentCaptor.capture()))
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
            XeroCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNotNull(result);
        assertEquals(propertyStubsMap, bodyArgumentCaptor.getValue()
            .getContent());

        // todo: somehow validate lambda parameter of http method
//        Context.ContextFunction<Context.Http, ?> expectedFunction =
//            http -> http.post("https://api.xero.com/api.xro/2.0/Contacts");
//        assertEquals(expectedFunction, functionArgumentCaptor.getValue());
    }
}
