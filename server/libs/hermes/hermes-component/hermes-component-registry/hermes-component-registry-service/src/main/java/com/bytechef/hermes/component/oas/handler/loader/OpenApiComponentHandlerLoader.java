
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

package com.bytechef.hermes.component.oas.handler.loader;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.hermes.component.definition.ActionDefinitionWrapper;
import com.bytechef.hermes.component.handler.loader.AbstractComponentHandlerLoader;
import com.bytechef.hermes.component.oas.handler.OpenApiComponentTaskHandler;
import com.bytechef.hermes.component.util.OpenApiClientUtils;

import java.util.List;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
public class OpenApiComponentHandlerLoader extends AbstractComponentHandlerLoader<OpenApiComponentHandler> {

    public static final Function<ActionDefinition, PerformFunction> PERFORM_FUNCTION_FUNCTION =
        actionDefinition -> (inputParameters, context) -> OpenApiClientUtils.execute(
            inputParameters, OptionalUtils.orElse(actionDefinition.getProperties(), List.of()),
            OptionalUtils.orElse(actionDefinition.getOutputSchema(), null),
            OptionalUtils.orElse(actionDefinition.getMetadata(), null));

    public OpenApiComponentHandlerLoader() {
        super(
            (componentHandler, actionDefinition) -> new ActionDefinitionWrapper(
                actionDefinition, () -> PERFORM_FUNCTION_FUNCTION.apply(actionDefinition)),
            OpenApiComponentHandler.class);
    }

    @Override
    protected ComponentTaskHandlerFunction getComponentTaskHandlerFunction(OpenApiComponentHandler componentHandler) {
        return (actionName, actionDefinitionService) -> new OpenApiComponentTaskHandler(actionName,
            actionDefinitionService, componentHandler);
    }
}
