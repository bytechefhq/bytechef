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

package com.bytechef.component.loops;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.loops.action.LoopsCreateContactAction;
import com.bytechef.component.loops.connection.LoopsConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class LoopsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("loops")
        .title("Loops")
        .description(
            "Loops is an email marketing and transactional email platform built for modern SaaS companies, helping " +
                "businesses automate onboarding, product updates, and lifecycle messaging with simple workflows, API " +
                "integrations, and scalable contact management.")
        .icon("path:assets/loops.svg")
        .categories(ComponentCategory.ADVERTISING)
        .connection(LoopsConnection.CONNECTION_DEFINITION)
        .actions(LoopsCreateContactAction.ACTION_DEFINITION)
        .version(1)
        .customAction(true)
        .customActionHelp("", "https://loops.so/docs/api-reference/intro");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
