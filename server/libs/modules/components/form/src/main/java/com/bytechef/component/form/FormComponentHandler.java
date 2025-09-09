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

package com.bytechef.component.form;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.form.constant.FormConstants.FORM;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.form.trigger.NewFormRequestTrigger;
import com.google.auto.service.AutoService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author ByteChef
 */
@AutoService(ComponentHandler.class)
public class FormComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(FORM)
        .title("Form")
        .description("Component for handling form submissions and requests.")
        .icon("path:assets/form.svg")
        .categories(ComponentCategory.HELPERS)
        .triggers(NewFormRequestTrigger.TRIGGER_DEFINITION);

    @Override
    @SuppressFBWarnings("EI")
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
