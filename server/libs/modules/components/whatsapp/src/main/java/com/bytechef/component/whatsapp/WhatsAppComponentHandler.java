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

package com.bytechef.component.whatsapp;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.whatsapp.connection.WhatsAppConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.WHATS_APP;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.whatsapp.action.WhatsAppSendMessageAction;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 */
@AutoService(ComponentHandler.class)
public class WhatsAppComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(WHATS_APP)
        .title("WhatsApp")
        .description(
            "WhatsApp is a free-to-use messaging app offering end-to-end encrypted chat, voice, and " +
                "video communication, along with document and media sharing, available on multiple platforms.")
        .connection(CONNECTION_DEFINITION)
        .categories(ComponentCategory.COMMUNICATION)
        .actions(WhatsAppSendMessageAction.ACTION_DEFINITION)
        .icon("path:assets/whatsapp.svg");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
