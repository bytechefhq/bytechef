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

package com.bytechef.component.merge;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.merge.action.MergeAppendAction;
import com.bytechef.component.merge.action.MergeCombineAction;
import com.bytechef.component.merge.action.MergeSQLQueryAction;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class MergeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("merge")
        .title("Merge")
        .description("Combine multiple inputs into one output.")
        .icon("path:assets/merge.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            MergeAppendAction.ACTION_DEFINITION,
            MergeCombineAction.ACTION_DEFINITION,
            MergeSQLQueryAction.ACTION_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
