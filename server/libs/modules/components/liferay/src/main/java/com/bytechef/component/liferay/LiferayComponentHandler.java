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

package com.bytechef.component.liferay;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.liferay.action.LiferayHeadlessAction;
import com.bytechef.component.liferay.connection.LiferayConnection;
import com.google.auto.service.AutoService;

/**
 * @author Igor Beslic
 */
@AutoService(ComponentHandler.class)
public class LiferayComponentHandler implements ComponentHandler {

    public static final ComponentDefinition COMPONENT_DEFINITION = component("liferay")
        .title("Liferay")
        .description(
            "Liferay is an open-source digital experience platform for enterprise content management (ECM) and portal development.")
        .icon("path:assets/liferay.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(LiferayConnection.CONNECTION_DEFINITION)
        .actions(LiferayHeadlessAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
