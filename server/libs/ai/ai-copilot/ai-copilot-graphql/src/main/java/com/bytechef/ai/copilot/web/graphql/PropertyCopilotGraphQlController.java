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

package com.bytechef.ai.copilot.web.graphql;

import com.bytechef.ai.copilot.CopilotGenerationException;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.facade.PropertyCopilotFacade;
import com.bytechef.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class PropertyCopilotGraphQlController {

    private final PropertyCopilotFacade propertyCopilotFacade;

    public PropertyCopilotGraphQlController(PropertyCopilotFacade propertyCopilotFacade) {
        this.propertyCopilotFacade = propertyCopilotFacade;
    }

    @MutationMapping
    public GeneratePropertyValuePayload generatePropertyValue(@Argument GeneratePropertyValueInput input) {
        PropertyCopilotRequest request = new PropertyCopilotRequest(
            input.prompt(), input.mode(), input.workflowId(), input.workflowNodeName(), input.propertyPath(),
            input.propertyType(), input.dynamic(), input.environmentId());

        PropertyCopilotResult result;

        try {
            result = propertyCopilotFacade.generatePropertyValue(request);
        } catch (ChatModelUnavailableException | CopilotGenerationException exception) {
            throw new GraphQlBadRequestException(exception.getMessage(), exception);
        }

        return new GeneratePropertyValuePayload(result.value(), result.valid(), result.message());
    }

    @SuppressFBWarnings("EI")
    public record GeneratePropertyValueInput(
        String prompt, PropertyCopilotMode mode, String workflowId, String workflowNodeName, String propertyPath,
        String propertyType, boolean dynamic, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GeneratePropertyValuePayload(String value, boolean valid, String message) {
    }
}
