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

package com.bytechef.component.mergehelper;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.mergehelper.action.MergeHelperAppendAction;
import com.bytechef.component.mergehelper.action.MergeHelperCombineAction;
import com.bytechef.component.mergehelper.action.MergeHelperSQLQueryAction;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class MergeHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mergeHelper")
        .title("Merge Helper")
        .description("Combine multiple inputs into one output.")
        .icon("path:assets/merge-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            MergeHelperAppendAction.ACTION_DEFINITION,
            MergeHelperCombineAction.ACTION_DEFINITION,
            MergeHelperSQLQueryAction.ACTION_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
