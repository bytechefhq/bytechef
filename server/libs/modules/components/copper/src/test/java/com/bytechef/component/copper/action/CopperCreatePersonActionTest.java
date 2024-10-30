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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.action.CopperCreatePersonAction.POST_PEOPLE_CONTEXT_FUNCTION;
import static com.bytechef.component.copper.constant.CopperConstants.ADDRESS;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY;
import static com.bytechef.component.copper.constant.CopperConstants.CITY;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.NUMBER;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS;
import static com.bytechef.component.copper.constant.CopperConstants.STREET;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.TITLE;
import static com.bytechef.component.copper.constant.CopperConstants.URL;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class CopperCreatePersonActionTest extends AbstractCopperActionTest {

    private static final List<Map<String, String>> phoneNumbers = List.of(Map.of(NUMBER, "1234", CATEGORY, "work"));
    private static final List<Map<String, String>> socials = List.of(Map.of(URL, "url", CATEGORY, "youtube"));
    private static final List<Map<String, String>> websites = List.of(Map.of(URL, "url", CATEGORY, "personal"));

    @Test
    void testPerform() {
        Map<String, Object> bodyMap = Map.of(
            NAME, "name", ASSIGNEE_ID, "assigneeId", TITLE, "title", DETAILS, "details", CONTACT_TYPE_ID, "contactType",
            PHONE_NUMBERS, phoneNumbers, SOCIALS, socials, WEBSITES, websites,
            ADDRESS, Map.of(STREET, "street", CITY, "city"), TAGS, List.of("tag1", "tag2"));

        mockedParameters = MockParametersFactory.create(bodyMap);

        Object result = CopperCreatePersonAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        verify(mockedContext, times(1)).http(POST_PEOPLE_CONTEXT_FUNCTION);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(bodyMap, body.getContent());
    }
}
