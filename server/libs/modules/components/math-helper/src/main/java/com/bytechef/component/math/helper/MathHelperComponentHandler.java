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

package com.bytechef.component.math.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.math.helper.action.MathHelperAdditionAction;
import com.bytechef.component.math.helper.action.MathHelperDivisionAction;
import com.bytechef.component.math.helper.action.MathHelperModuloAction;
import com.bytechef.component.math.helper.action.MathHelperMultiplicationAction;
import com.bytechef.component.math.helper.action.MathHelperSubtractionAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class MathHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mathHelper")
        .title("Math Helper")
        .description("Helper component to perform mathematical operations.")
        .icon("path:assets/math-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            MathHelperAdditionAction.ACTION_DEFINITION,
            MathHelperDivisionAction.ACTION_DEFINITION,
            MathHelperModuloAction.ACTION_DEFINITION,
            MathHelperMultiplicationAction.ACTION_DEFINITION,
            MathHelperSubtractionAction.ACTION_DEFINITION)
        .clusterElements(
            tool(MathHelperAdditionAction.ACTION_DEFINITION),
            tool(MathHelperDivisionAction.ACTION_DEFINITION),
            tool(MathHelperModuloAction.ACTION_DEFINITION),
            tool(MathHelperMultiplicationAction.ACTION_DEFINITION),
            tool(MathHelperSubtractionAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
