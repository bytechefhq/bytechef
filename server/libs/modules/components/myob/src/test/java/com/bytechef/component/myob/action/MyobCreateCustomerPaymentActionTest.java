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

package com.bytechef.component.myob.action;

import static com.bytechef.component.myob.constant.MyobConstants.ACCOUNT;
import static com.bytechef.component.myob.constant.MyobConstants.CUSTOMER;
import static com.bytechef.component.myob.constant.MyobConstants.PAY_FROM;
import static com.bytechef.component.myob.constant.MyobConstants.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.test.component.properties.ParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class MyobCreateCustomerPaymentActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        Parameters parameters = ParametersFactory.createParameters(Map.of(PAY_FROM, ACCOUNT,
            ACCOUNT, "123", CUSTOMER, "abc"));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);

        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object result = MyobCreateCustomerPaymentAction.perform(parameters, parameters, mockedContext);

        assertNull(result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(PAY_FROM, ACCOUNT, ACCOUNT, Map.of(UID, "123"), CUSTOMER, Map.of(UID, "abc")),
            body.getContent());
    }
}
