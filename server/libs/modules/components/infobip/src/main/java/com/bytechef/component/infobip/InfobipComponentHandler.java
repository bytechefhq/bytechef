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

package com.bytechef.component.infobip;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.infobip.constant.InfobipConstants.INFOBIP;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.infobip.action.InfobipSendSMSAction;
import com.bytechef.component.infobip.action.InfobipSendWhatsappTextMesageAction;
import com.bytechef.component.infobip.connection.InfobipConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class InfobipComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(INFOBIP)
        .title("Infobip")
        .description(
            "Infobip is a global communications platform that provide cloud-based messaging and omnichannel " +
                "communication solutions for businesses.")
        .icon("path:assets/infobip.svg")
        .connection(InfobipConnection.CONNECTION_DEFINITION)
        .actions(
            InfobipSendSMSAction.ACTION_DEFINITION,
            InfobipSendWhatsappTextMesageAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
