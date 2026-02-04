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

package com.bytechef.component.object.helper;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.object.helper.action.ObjectHelperAddKeyValuePairsAction;
import com.bytechef.component.object.helper.action.ObjectHelperAddValueByKeyAction;
import com.bytechef.component.object.helper.action.ObjectHelperContainsAction;
import com.bytechef.component.object.helper.action.ObjectHelperDeleteKeyValuePairAction;
import com.bytechef.component.object.helper.action.ObjectHelperEqualsAction;
import com.bytechef.component.object.helper.action.ObjectHelperMergeTwoObjectsAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ObjectHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("objectHelper")
        .title("Object Helper")
        .description("Object Helper allows you to do various operations on objects.")
        .icon("path:assets/object-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            ObjectHelperAddValueByKeyAction.ACTION_DEFINITION,
            ObjectHelperAddKeyValuePairsAction.ACTION_DEFINITION,
            ObjectHelperContainsAction.ACTION_DEFINITION,
            ObjectHelperDeleteKeyValuePairAction.ACTION_DEFINITION,
            ObjectHelperEqualsAction.ACTION_DEFINITION,
            ObjectHelperMergeTwoObjectsAction.ACTION_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
