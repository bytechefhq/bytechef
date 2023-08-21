
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

package com.bytechef.hermes.component.handler.loader;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.ComponentHandler.ActionHandlerFunction;
import com.bytechef.hermes.component.definition.ActionDefinitionWrapper;
import com.bytechef.hermes.component.handler.ComponentTaskHandler;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentHandlerLoader extends AbstractComponentHandlerLoader<ComponentHandler> {

    public DefaultComponentHandlerLoader() {
        super(
            (componentHandler, actionDefinition) -> {
                if (OptionalUtils.isPresent(componentHandler.getActionHandler())) {
                    return new ActionDefinitionWrapper(
                        actionDefinition,
                        () -> {
                            ActionHandlerFunction actionHandlerFunction = OptionalUtils.get(
                                componentHandler.getActionHandler());

                            return (inputParameters, context) -> actionHandlerFunction.apply(
                                actionDefinition.getName(), inputParameters, context);
                        });
                } else {
                    return actionDefinition;
                }
            },
            ComponentHandler.class);
    }

    @Override
    protected ComponentTaskHandlerFunction getComponentTaskHandlerFunction(ComponentHandler componentHandler) {
        return (actionName, actionDefinitionService) -> new ComponentTaskHandler(
            componentHandler.getName(), componentHandler.getVersion(), actionName, actionDefinitionService);
    }
}
