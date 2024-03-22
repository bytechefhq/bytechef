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

package com.bytechef.component.zendesk.sell.util;

import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.FIRST_NAME_PROPERTY;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.IS_ORGANIZATION;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.LAST_NAME_PROPERTY;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.ORGANIZATION_NAME_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class ZendeskSellUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testCreateNamePropertiesForPerson() {
        when(mockedParameters.getRequiredBoolean(IS_ORGANIZATION))
                .thenReturn(false);

        List<Property.ValueProperty<?>> nameProperties = ZendeskSellUtils.createNameProperties(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(2, nameProperties.size());

        assertEquals(FIRST_NAME_PROPERTY, nameProperties.getFirst());
        assertEquals(LAST_NAME_PROPERTY, nameProperties.get(1));
    }

    @Test
    void testCreateNamePropertiesForOrganization() {
        when(mockedParameters.getRequiredBoolean(IS_ORGANIZATION))
            .thenReturn(true);

        List<Property.ValueProperty<?>> nameProperties = ZendeskSellUtils.createNameProperties(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, nameProperties.size());

        assertEquals(ORGANIZATION_NAME_PROPERTY, nameProperties.getFirst());
    }
}
