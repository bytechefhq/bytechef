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

package com.bytechef.component.claude.code.action;

import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.AUTHENTICATION;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.AUTHENTICATION_TYPE;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.LABEL;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.NAME;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.URL;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.VALUE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.claude.code.util.ClaudeCodeUtil;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class ClaudeCodeAddMCPAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addMCP")
        .title("Add MCP Server")
        .description("Builds an HTTP-transport MCP server descriptor that can be passed to the Chat action.")
        .properties(
            string(LABEL)
                .label("Label")
                .description("The name of the MCP server")
                .required(true),
            string(URL)
                .label("URL")
                .description("The URL of the MCP server")
                .required(true),
            integer(AUTHENTICATION_TYPE)
                .label("Authentication Type")
                .description("The type of authentication to use for connecting to the MCP server")
                .options(
                    option("None", 0),
                    option("Access Token/API key", 1),
                    option("Custom Headers", 2))
                .required(true),
            string(AUTHENTICATION)
                .label("Authentication")
                .description("The access token/API key to use for authentication")
                .displayCondition("authenticationType == 1")
                .required(true),
            array(AUTHENTICATION)
                .label("Authentication")
                .description("The custom headers to use for authentication")
                .displayCondition("authenticationType == 2")
                .items(
                    object()
                        .properties(
                            string(NAME)
                                .label("Header Name")
                                .required(true),
                            string(VALUE)
                                .label("Header Value")
                                .required(true))))
        .output(
            outputSchema(
                string()
                    .description("Serialized MCP server descriptor consumable by the Chat action.")),
            sampleOutput("Sample result"))
        .perform(ClaudeCodeAddMCPAction::perform);

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, String> headers = buildHeaders(inputParameters);
        McpServerConfig.McpHttpServerConfig serverConfig = new McpServerConfig.McpHttpServerConfig(
            inputParameters.getRequiredString(URL), headers);

        return ClaudeCodeUtil.serializeMcpServer(inputParameters.getRequiredString(LABEL), serverConfig);
    }

    private static Map<String, String> buildHeaders(Parameters inputParameters) {
        Map<String, String> headers = new LinkedHashMap<>();
        int authType = inputParameters.getInteger(AUTHENTICATION_TYPE);

        switch (authType) {
            case 1:
                headers.put(AUTHENTICATION, "Bearer " + inputParameters.getString(AUTHENTICATION));

                break;
            case 2:
                List<Authentication> authentications = inputParameters.getList(
                    AUTHENTICATION, Authentication.class, List.of());

                for (Authentication authentication : authentications) {
                    headers.put(authentication.name(), authentication.value());
                }

                break;
            default:
        }

        return headers;
    }

    public record Authentication(String name, String value) {
    }
}
