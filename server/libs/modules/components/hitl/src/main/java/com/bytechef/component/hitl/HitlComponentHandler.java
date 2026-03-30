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

package com.bytechef.component.hitl;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.hitl.constant.HitlConstants.HITL;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.hitl.action.ApproveAction;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.HitlComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(HITL + "_v1_ComponentHandler")
public class HitlComponentHandler implements ComponentHandler {

    private final HitlComponentDefinition componentDefinition;

    public HitlComponentHandler() {
        this.componentDefinition = new HitlComponentDefinitionImpl(
            component(HITL)
                .title("Human in the Loop")
                .description("Human-in-the-Loop component for manual intervention in workflows.")
                .icon("path:assets/hitl.svg")
                .categories(ComponentCategory.HELPERS)
                .actions(ApproveAction.ACTION_DEFINITION));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class HitlComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements HitlComponentDefinition {

        public HitlComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
