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

import static com.bytechef.component.xero.constant.XeroConstants.ACCPAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.xero.util.XeroUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class XeroCreateBillActionTest extends AbstractXeroActionTest {

    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        try (MockedStatic<XeroUtils> xeroUtilsMockedStatic = mockStatic(XeroUtils.class)) {
            xeroUtilsMockedStatic
                .when(() -> XeroUtils.createInvoice(mockedParameters, mockedContext,
                    XeroCreateInvoiceAction.POST_INVOICES_CONTEXT_FUNCTION, ACCPAY))
                .thenReturn(mockedObject);

            Object result = XeroCreateBillAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
        }
    }
}
