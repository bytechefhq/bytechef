/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.google.workspace.admin.action;

import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.PRODUCT_ID;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.SKU_ID;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class GoogleWorkspaceAdminAssignLicenseActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Context.Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(USER_ID, "user-1", PRODUCT_ID, "product-1", SKU_ID, "sku-1"));
    private final Response mockedResponse = mock(Response.class);
    private final Map<String, Object> responseMap = Map.of("result", List.of("123", "abc"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(responseMap);

        Object result =
            GoogleWorkspaceAdminAssignLicenseAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals("https://licensing.googleapis.com/apps/licensing/v1/product/product-1/sku/sku-1/user",
            stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of(USER_ID, "user-1"), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
