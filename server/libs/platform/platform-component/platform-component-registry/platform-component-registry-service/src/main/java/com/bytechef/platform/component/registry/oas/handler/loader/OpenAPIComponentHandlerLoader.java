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

package com.bytechef.platform.component.registry.oas.handler.loader;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.OpenAPIComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.platform.component.definition.ActionDefinitionWrapper;
import com.bytechef.platform.component.registry.handler.loader.AbstractComponentHandlerLoader;
import com.bytechef.platform.component.registry.oas.handler.OpenAPIComponentTaskHandler;
import com.bytechef.platform.component.util.OpenAPIClientUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Ivica Cardic
 */
public class OpenAPIComponentHandlerLoader extends AbstractComponentHandlerLoader<OpenAPIComponentHandler> {

    public static final Function<ActionDefinition, SingleConnectionPerformFunction> PERFORM_FUNCTION_FUNCTION =
        actionDefinition -> (inputParameters, connectionParameters, context) -> OpenAPIClientUtils.execute(
            inputParameters, OptionalUtils.orElse(actionDefinition.getProperties(), List.of()),
            OptionalUtils.orElse(actionDefinition.getOutputDefinition(), null),
            OptionalUtils.orElse(actionDefinition.getMetadata(), Map.of()),
            OptionalUtils.orElse(actionDefinition.getProcessErrorResponse(), null), context);

    public OpenAPIComponentHandlerLoader() {
        super(
            (componentHandler, actionDefinition) -> {
                if (OptionalUtils.isPresent(actionDefinition.getPerform())) {
                    return actionDefinition;
                } else {
                    return new ActionDefinitionWrapper(
                        actionDefinition, PERFORM_FUNCTION_FUNCTION.apply(actionDefinition));
                }
            },
            OpenAPIComponentHandler.class);
    }

    @Override
    protected ComponentTaskHandlerFunction getComponentTaskHandlerFunction(OpenAPIComponentHandler componentHandler) {
        return (actionName, actionDefinitionFacade) -> new OpenAPIComponentTaskHandler(
            actionName, actionDefinitionFacade, componentHandler);
    }
}
