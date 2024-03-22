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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class ZendeskSellUtils {

    private ZendeskSellUtils() {
    }

    public static List<Property.ValueProperty<?>> createNameProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        boolean isOrganization = inputParameters.getRequiredBoolean(IS_ORGANIZATION);

        if (isOrganization) {
            return List.of(ORGANIZATION_NAME_PROPERTY);
        } else {
            return List.of(FIRST_NAME_PROPERTY, LAST_NAME_PROPERTY);
        }
    }
}
