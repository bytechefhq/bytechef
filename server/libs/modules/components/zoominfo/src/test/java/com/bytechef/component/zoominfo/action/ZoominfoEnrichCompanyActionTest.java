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

package com.bytechef.component.zoominfo.action;

import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_CITY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_COUNTRY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_ID;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_PHONE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_STATE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_STREET;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_WEBSITE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_ZIPCODE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.OUTPUT_FIELDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
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
class ZoominfoEnrichCompanyActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Context.ContextFunction<Context.Http, Executor>> httpFunctionArgumentCaptor =
        forClass(Context.ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(COMPANY_ID, 1, COMPANY_NAME, "test", COMPANY_WEBSITE, "http://www.example.com",
            COMPANY_PHONE, "+123456789", COMPANY_STREET, "Test Street", COMPANY_CITY, "City",
            COMPANY_STATE, "State", COMPANY_ZIPCODE, "100", COMPANY_COUNTRY, "Country",
            OUTPUT_FIELDS, List.of("id", "name", "website")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                Context.ContextFunction<Http, Executor> value = httpFunctionArgumentCaptor.getValue();

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
            .thenReturn(mockedObject);

        Object result = ZoominfoEnrichCompanyAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        Context.ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        Map<String, Object> attributes = Map.of(
            OUTPUT_FIELDS, List.of("id", "name", "website"),
            "matchCompanyInput", Map.of(COMPANY_ID, 1, COMPANY_NAME, "test",
                COMPANY_WEBSITE, "http://www.example.com", COMPANY_PHONE, "+123456789",
                COMPANY_STREET, "Test Street", COMPANY_CITY, "City", COMPANY_STATE, "State",
                COMPANY_ZIPCODE, "100", COMPANY_COUNTRY, "Country"));

        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals("/companies/enrich", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                Map.of("data", Map.of("type", "CompanyEnrich",
                    "attributes", attributes)),
                Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
