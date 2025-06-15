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

package com.bytechef.component.calcom;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.calcom.connection.CalComConnection;
import com.bytechef.component.calcom.trigger.CalComBookingCancelledTrigger;
import com.bytechef.component.calcom.trigger.CalComBookingCreatedTrigger;
import com.bytechef.component.calcom.trigger.CalComBookingEndedTrigger;
import com.bytechef.component.calcom.trigger.CalComBookingRescheduledTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class CalComComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("calcom")
        .title("Cal.com")
        .description(
            "A fully customizable scheduling software for individuals, businesses taking calls and developers " +
                "building scheduling platforms where users meet users.")
        .icon("path:assets/calcom.svg")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(CalComConnection.CONNECTION_DEFINITION)
        .triggers(
            CalComBookingCancelledTrigger.TRIGGER_DEFINITION,
            CalComBookingCreatedTrigger.TRIGGER_DEFINITION,
            CalComBookingEndedTrigger.TRIGGER_DEFINITION,
            CalComBookingRescheduledTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
