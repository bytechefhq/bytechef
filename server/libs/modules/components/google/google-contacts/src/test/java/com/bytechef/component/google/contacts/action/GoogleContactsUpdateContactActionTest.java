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
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PERSON_FIELDS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBERS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType.Type;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Martin Tarasovič
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
public class GoogleContactsUpdateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(RESOURCE_NAME, "resourceName", GIVEN_NAME, "givenName", FAMILY_NAME, "familyName",
            TITLE, "title", NAME, "name", EMAIL, "email"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> personMap = Map.of(
            GIVEN_NAME, "givenName", FAMILY_NAME, "familyName", TITLE, "title", NAME, "name",
            EMAIL, "email", E_TAG, "etag");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.patch(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(personMap);
        when(mockedResponse.getBody())
            .thenReturn(personMap);

        Object result = GoogleContactsUpdateContactAction.perform(mockedParameters, null, mockedContext);

        assertEquals(personMap, result);

        for (ContextFunction<Http, Executor> function : httpFunctionArgumentCaptor.getAllValues()) {
            assertNotNull(function);
        }

        for (ConfigurationBuilder configurationBuilder : configurationBuilderArgumentCaptor.getAllValues()) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(Type.JSON, configuration.getResponseType()
                .getType());
        }

        Map<String, Object> expectedBodyMap = Map.of(
            "etag", "etag",
            NAMES, List.of(Map.of(GIVEN_NAME, "givenName", FAMILY_NAME, "familyName")),
            ORGANIZATIONS, List.of(Map.of(NAME, "name", TITLE, "title")),
            EMAIL_ADDRESSES, List.of(Map.of(VALUE, "email")),
            PHONE_NUMBERS, List.of(Map.of()));

        List<String> expectedStrings = List.of(
            "/resourceName", PERSON_FIELDS, "emailAddresses,names,phoneNumbers,organizations",
            "/resourceName:updateContact", "updatePersonFields",
            "emailAddresses,names,phoneNumbers,organizations");

        assertEquals(expectedStrings, stringArgumentCaptor.getAllValues());
        assertEquals(Body.of(expectedBodyMap, BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}
