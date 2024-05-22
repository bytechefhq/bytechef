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

package com.bytechef.component.nifty;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.nifty.connection.NiftyConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.nifty.constant.NiftyConstants.NIFTY;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.nifty.action.NiftyCreateTaskAction;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 */
@AutoService(ComponentHandler.class)
public class NiftyComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(NIFTY)
        .title("Nifty")
        .description(
            "Nifty Project Management tool is a software designed to aid project managers in organizing, planning, " +
                "and tracking tasks and resources within a project")
        .categories(ComponentCategory.PROJECT_MANAGEMENT)
        .connection(CONNECTION_DEFINITION)
        .actions(NiftyCreateTaskAction.ACTION_DEFINITION)
        .icon("path:assets/nifty.svg");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
