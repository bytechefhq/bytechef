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

package com.bytechef.component.freshsales.action;

import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ADDRESS;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.CITY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.COUNTRY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.EMAIL;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FACEBOOK;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FIRST_NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.JOB_TITLE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LAST_NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LINKEDIN;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.MEDIUM;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.MOBILE_NUMBER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.STATE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.TWITTER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.WORK_NUMBER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ZIPCODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
@ExtendWith(MockContextSetupExtension.class)
class FreshsalesCreateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(FIRST_NAME, "firstName"),
            Map.entry(LAST_NAME, "lastName"),
            Map.entry(JOB_TITLE, "jobTitle"),
            Map.entry(EMAIL, "email"),
            Map.entry(WORK_NUMBER, "workNumber"),
            Map.entry(MOBILE_NUMBER, "mobileNumber"),
            Map.entry(ADDRESS, "address"),
            Map.entry(CITY, "city"),
            Map.entry(STATE, "state"),
            Map.entry(ZIPCODE, "zipcode"),
            Map.entry(COUNTRY, "country"),
            Map.entry(MEDIUM, "medium"),
            Map.entry(FACEBOOK, "facebook"),
            Map.entry(TWITTER, "twitter"),
            Map.entry(LINKEDIN, "linkedin")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Executor mockedExecutor, Http mockedHttp, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of());

        Object result = FreshsalesCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/contacts", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                FIRST_NAME, "firstName", LAST_NAME, "lastName", JOB_TITLE, "jobTitle",
                EMAIL, "email", WORK_NUMBER, "workNumber", MOBILE_NUMBER, "mobileNumber", ADDRESS, "address",
                CITY, "city", STATE, "state", ZIPCODE, "zipcode", COUNTRY, "country", MEDIUM, "medium",
                FACEBOOK, "facebook", TWITTER, "twitter", LINKEDIN, "linkedin"),
            bodyArgumentCaptor.getValue());
    }
}
