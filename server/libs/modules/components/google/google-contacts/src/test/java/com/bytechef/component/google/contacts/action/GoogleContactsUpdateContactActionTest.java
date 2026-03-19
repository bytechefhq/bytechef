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

package com.bytechef.component.google.contacts.action;

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL_ADDRESSES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.E_TAG;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAMES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.ORGANIZATIONS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBERS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.contacts.util.GoogleContactsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Martin Tarasovič
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
public class GoogleContactsUpdateContactActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(RESOURCE_NAME, "resourceName", GIVEN_NAME, "givenName", FAMILY_NAME, "familyName",
            TITLE, "title", NAME, "name", EMAIL, "email"));
    private final Map<String, Object> responseMap = Map.of("results", List.of());
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (
            MockedStatic<GoogleContactsUtils> googleContactsUtilsMockedStatic = mockStatic(GoogleContactsUtils.class)) {
            googleContactsUtilsMockedStatic
                .when(() -> GoogleContactsUtils.getContactToUpdate(stringArgumentCaptor.capture(),
                    contextArgumentCaptor.capture()))
                .thenReturn(
                    Map.of(
                        E_TAG, "etag", NAMES, new HashMap<>(), ORGANIZATIONS, new HashMap<>(),
                        EMAIL_ADDRESSES, new HashMap<>(), PHONE_NUMBERS, new HashMap<>()));

            when(mockedHttp.patch(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                    .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            Object result = GoogleContactsUpdateContactAction.perform(
                mockedParameters, null, mockedContext);

            assertEquals(responseMap, result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();
            ResponseType responseType = configuration.getResponseType();

            Map<String, Object> expectedBodyMap = Map.of(
                "etag", "etag",
                NAMES, List.of(Map.of(GIVEN_NAME, "givenName", FAMILY_NAME, "familyName")),
                ORGANIZATIONS, List.of(Map.of(NAME, "name", TITLE, "title")),
                EMAIL_ADDRESSES, List.of(Map.of(VALUE, "email")),
                PHONE_NUMBERS, List.of(Map.of()));

            List<String> expectedStrings = List.of(
                "resourceName", "/resourceName:updateContact", "updatePersonFields",
                "emailAddresses,names,phoneNumbers,organizations");

            assertEquals(ResponseType.Type.JSON, responseType.getType());
            assertEquals(expectedStrings, stringArgumentCaptor.getAllValues());
            assertEquals(Http.Body.of(expectedBodyMap, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
