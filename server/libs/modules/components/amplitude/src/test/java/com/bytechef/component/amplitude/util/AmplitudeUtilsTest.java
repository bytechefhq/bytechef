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

package com.bytechef.component.amplitude.util;

import static com.bytechef.component.amplitude.constant.AmplitudeConstants.API_KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.EVENT_TYPE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.ID;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.IDENTIFIER;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.PLATFORM;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_PROPERTIES;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.VALUE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class AmplitudeUtilsTest {
    private static final Context mockedContext = mock(Context.class);
    private static final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            API_KEY, "api_key", EVENT_TYPE, "eventType", PLATFORM, "platform", ID, "id",
            IDENTIFIER, Map.of(KEY, "identifierKey", VALUE, "identifierValue"),
            USER_PROPERTIES, List.of(Map.of(KEY, "userPropertyKey", VALUE, "userPropertyValue"))));
    private static final String responseString = "response";

    @Test
    void testGetEventJson() {
        when(mockedContext.json(any()))
            .thenReturn(responseString);

        String result = AmplitudeUtils.getEventJson(mockedParameters, mockedContext);

        assertEquals(responseString, result);
    }

    @Test
    void testGetIdentification() {
        Map<String, Object> identification = AmplitudeUtils.getIdentification(mockedParameters);

        Map<String, Object> expectedIdentification = Map.of(
            ID, ID, USER_PROPERTIES, Map.of("userPropertyKey", "userPropertyValue"));

        assertEquals(expectedIdentification, identification);
    }

    @Test
    void testGetIdentifierKeyOptionsIos() {
        Parameters parameters = MockParametersFactory.create(Map.of(PLATFORM, "ios"));

        List<Option<String>> result = AmplitudeUtils.getIdentifierKeyOptions(
            parameters, parameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("The Identifier for Advertiser", "idfa"),
            option("The Identifier for Vendor", "idfv"));

        assertEquals(expected, result);
    }

    @Test
    void testGetIdentifierKeyOptionsAndroid() {
        Parameters parameters = MockParametersFactory.create(Map.of(PLATFORM, "android"));

        List<Option<String>> result = AmplitudeUtils.getIdentifierKeyOptions(
            parameters, parameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("The Google ADID", "adid"),
            option("App Set ID", "android_app_set_id"));

        assertEquals(expected, result);
    }

    @Test
    void testGetUserProperties() {
        Map<String, String> result = AmplitudeUtils.getUserProperties(mockedParameters);

        Map<String, String> expected = Map.of("userPropertyKey", "userPropertyValue");

        assertEquals(expected, result);
    }
}
