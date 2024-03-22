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

package com.bytechef.component.zendesk.sell.action;

import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.DATA;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.EMAIL;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.FIRST_NAME;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.IS_ORGANIZATION;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.LAST_NAME;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.TITLE;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.WEBSITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class ZendeskSellCreateContactActionTest extends AbstractZendeskSellctionTest {

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredBoolean(IS_ORGANIZATION))
            .thenReturn((Boolean) propertyStubsMap.get(IS_ORGANIZATION));
        when(mockedParameters.getString(FIRST_NAME))
            .thenReturn((String) propertyStubsMap.get(FIRST_NAME));
        when(mockedParameters.getString(LAST_NAME))
            .thenReturn((String) propertyStubsMap.get(LAST_NAME));
        when(mockedParameters.getString(TITLE))
            .thenReturn((String) propertyStubsMap.get(TITLE));
        when(mockedParameters.getString(WEBSITE))
            .thenReturn((String) propertyStubsMap.get(WEBSITE));
        when(mockedParameters.getString(EMAIL))
            .thenReturn((String) propertyStubsMap.get(EMAIL));

        Object result = ZendeskSellCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(DATA, propertyStubsMap), body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(IS_ORGANIZATION, false);
        propertyStubsMap.put(TITLE, "title");
        propertyStubsMap.put(FIRST_NAME, "firstName");
        propertyStubsMap.put(LAST_NAME, "lastName");
        propertyStubsMap.put(WEBSITE, "website");
        propertyStubsMap.put(EMAIL, "mail@mail.com");

        return propertyStubsMap;
    }

}
