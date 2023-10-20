
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.objecthelper;

import static com.bytechef.component.objecthelper.constant.ObjectHelperConstants.OBJECT_HELPER;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.objecthelper.action.ObjectHelperParseAction;
import com.bytechef.component.objecthelper.action.ObjectHelperStringifyAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ObjectHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(OBJECT_HELPER)
        .title("Object Helper")
        .description("Object Helper allows you to do various operations on objects.")
        .icon("path:assets/objecthelper.svg")
        .actions(
            ObjectHelperParseAction.ACTION_DEFINITION,
            ObjectHelperStringifyAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
