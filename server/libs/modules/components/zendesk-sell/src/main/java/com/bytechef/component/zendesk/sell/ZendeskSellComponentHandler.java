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

package com.bytechef.component.zendesk.sell;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.zendesk.sell.connection.ZendeskSellConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.ZENDESK_SELL;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zendesk.sell.action.ZendeskSellCreateContactAction;
import com.bytechef.component.zendesk.sell.action.ZendeskSellCreateTaskAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class ZendeskSellComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(ZENDESK_SELL)
        .title("Zendesk Sell")
        .description(
            "Zendesk Sell is a sales CRM software that helps businesses manage leads, contacts, and deals efficiently.")
        .icon("path:assets/zendesk-sell.svg")
        .connection(CONNECTION_DEFINITION)
        .actions(
            ZendeskSellCreateContactAction.ACTION_DEFINITION,
            ZendeskSellCreateTaskAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
