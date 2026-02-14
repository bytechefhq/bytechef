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

package com.bytechef.platform.component.oas.handler.loader;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.platform.component.definition.ActionDefinitionWrapper;
import com.bytechef.platform.component.handler.loader.AbstractComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.OpenApiComponentTaskHandler;
import com.bytechef.platform.component.util.OpenApiClientUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
public class OpenApiComponentHandlerLoader extends AbstractComponentHandlerLoader<OpenApiComponentHandler> {

    public static final Function<ActionDefinition, PerformFunction> PERFORM_FUNCTION_FUNCTION =
        actionDefinition -> (inputParameters, connectionParameters, context) -> OpenApiClientUtils.execute(
            inputParameters,
            actionDefinition.getProperties()
                .orElse(List.of()),
            actionDefinition.getOutputDefinition()
                .orElse(null),
            actionDefinition.getMetadata()
                .orElse(Map.of()),
            actionDefinition.getProcessErrorResponse()
                .orElse(null),
            context);

    public OpenApiComponentHandlerLoader() {
        super(
            (componentHandler, actionDefinition) -> {
                Optional<? extends ActionDefinition.BasePerformFunction> perform = actionDefinition.getPerform();

                if (perform.isPresent()) {
                    return actionDefinition;
                } else {
                    return new ActionDefinitionWrapper(
                        actionDefinition, PERFORM_FUNCTION_FUNCTION.apply(actionDefinition));
                }
            },
            OpenApiComponentHandler.class);
    }

    @Override
    protected ComponentTaskHandlerFunction getComponentTaskHandlerFunction(OpenApiComponentHandler componentHandler) {
        return (actionName, actionDefinitionFacade) -> new OpenApiComponentTaskHandler(
            actionName, actionDefinitionFacade, componentHandler);
    }
}
