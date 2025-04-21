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

package com.bytechef.component.acumbamail;

import static com.bytechef.component.acumbamail.connection.AcumbamailConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.acumbamail.action.AcumbamailAddSubscriberAction;
import com.bytechef.component.acumbamail.action.AcumbamailCreateSubscriberListAction;
import com.bytechef.component.acumbamail.action.AcumbamailDeleteSubscriberAction;
import com.bytechef.component.acumbamail.action.AcumbamailDeleteSubscriberListAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class AcumbamailComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("acumbamail")
        .title("Acumbamail")
        .description(
            "Acumbamail is an email marketing and automation platform that allows users to create, manage, and " +
                "analyze email campaigns, newsletters, and SMS marketing with an intuitive interface and API integration.")
        .customAction(true)
        .icon("path:assets/acumbamail.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(CONNECTION_DEFINITION)
        .actions(
            AcumbamailAddSubscriberAction.ACTION_DEFINITION,
            AcumbamailDeleteSubscriberAction.ACTION_DEFINITION,
            AcumbamailCreateSubscriberListAction.ACTION_DEFINITION,
            AcumbamailDeleteSubscriberListAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
