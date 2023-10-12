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

package com.bytechef.hermes.component.handler.loader;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.ComponentHandler.ActionHandlerFunction;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.hermes.component.definition.ActionDefinitionWrapper;
import com.bytechef.hermes.component.handler.ComponentTaskHandler;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentHandlerLoader extends AbstractComponentHandlerLoader<ComponentHandler> {

    public static final BiFunction<ComponentHandler, ActionDefinition, PerformFunction> PERFORM_FUNCTION_FUNCTION =
        (componentHandler, actionDefinition) -> {
            ActionHandlerFunction actionHandlerFunction = OptionalUtils.get(
                componentHandler.getActionHandler());

            return (inputParameters, connectionParameters, context) -> actionHandlerFunction.apply(
                actionDefinition.getName(), inputParameters, context);
        };

    public DefaultComponentHandlerLoader() {
        super(
            (componentHandler, actionDefinition) -> {
                if (OptionalUtils.isPresent(componentHandler.getActionHandler())) {
                    return new ActionDefinitionWrapper(
                        actionDefinition, () -> PERFORM_FUNCTION_FUNCTION.apply(componentHandler, actionDefinition));
                } else {
                    return actionDefinition;
                }
            },
            ComponentHandler.class);
    }

    @Override
    protected ComponentTaskHandlerFunction getComponentTaskHandlerFunction(ComponentHandler componentHandler) {
        return (actionName, actionDefinitionFacade) -> new ComponentTaskHandler(
            componentHandler.getName(), componentHandler.getVersion(), actionName, actionDefinitionFacade);
    }
}
