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

package com.bytechef.component.myob.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.FIRST_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.LAST_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * * @author Monika Kušter
 */
class MyobUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters parameters = MockParametersFactory.create(Map.of());

    @BeforeEach()
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetCompanyFileOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("Name", "name", "Uri", "uri")));

        assertEquals(List.of(option("name", "uri")),
            MyobUtils.getCompanyFileOptions(parameters, parameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetCustomerOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("Items", List.of(Map.of(FIRST_NAME, "first", LAST_NAME, "last", UID, "123"))));

        assertEquals(List.of(option("first last", "123")),
            MyobUtils.getCustomerOptions(parameters, parameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetSupplierOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("Items", List.of(Map.of(COMPANY_NAME, "company", UID, "123"))));

        assertEquals(List.of(option("company", "123")),
            MyobUtils.getSupplierOptions(parameters, parameters, Map.of(), "", mockedActionContext));
    }

}
