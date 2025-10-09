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

import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.SCRIPT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.claude.code.util.ClaudeCodeUtil;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author Marko Kriskovic
 */
public class ClaudeCodeChat {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("chat")
        .title("Chat")
        .description("Chat with Claude with registered tools")
        .properties(
            string(SCRIPT)
                .label("Message")
                .description("Command to send to Claude")
                .required(true))
        .output(
            outputSchema(
                string()
                    .description(
                        "The output of the executed bash commands, including any standard output or error messages " +
                            "generated during execution.")),
            sampleOutput("Sample result"))
        .perform(ClaudeCodeChat::perform);

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws IOException, InterruptedException, TimeoutException {

        String command = "su - claude-user -c \"claude --dangerously-skip-permissions -p \""
            + inputParameters.getString(SCRIPT) + "\"\"";

        return ClaudeCodeUtil.execute(command);
    }
}
