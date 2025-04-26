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

package com.bytechef.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.embedded.execution.facade.ToolFacade;
import com.bytechef.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.execution.public_.web.rest.model.ExecuteToolRequestModel;
import com.bytechef.embedded.execution.public_.web.rest.model.FunctionModel;
import com.bytechef.embedded.execution.public_.web.rest.model.ToolModel;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class ToolApiController implements ToolApi {

    private final ToolFacade toolFacade;

    public ToolApiController(ToolFacade toolFacade) {
        this.toolFacade = toolFacade;
    }

    @Override
    public ResponseEntity<Object> executeTool(
        EnvironmentModel xEnvironment, Long xInstanceId, ExecuteToolRequestModel executeToolRequestModel) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            toolFacade.executeTool(
                executeToolRequestModel.getName(), executeToolRequestModel.getParameters(), environment, xInstanceId));
    }

    @Override
    public ResponseEntity<Map<String, List<ToolModel>>> getTools(
        String externalUserId, EnvironmentModel xEnvironment, List<String> categories, List<String> components,
        List<String> actions) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            toolFacade
                .getTools(
                    environment, categories == null ? List.of() : categories,
                    components == null ? List.of() : components, actions == null ? List.of() : actions)
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        toolEntry -> toolEntry.getValue()
                            .stream()
                            .map(ToolApiController::toToolModel)
                            .toList())));
    }

    private static ToolModel toToolModel(ToolDTO toolDTO) {
        return new ToolModel()
            .function(
                new FunctionModel()
                    .name(toolDTO.name())
                    .description(toolDTO.description())
                    .parameters(toolDTO.parameters()));
    }
}
