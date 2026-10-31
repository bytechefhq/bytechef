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

package com.bytechef.component.ai.agent.utils.cluster.subagent;

import com.bytechef.component.ai.llm.tool.ToolCallLoggingToolCallback;
import com.bytechef.component.definition.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

/**
 * @author Ivica Cardic
 */
public class ToolCallLoggingSubagentExecutor implements SubagentExecutor {

    private final Context context;
    private final SubagentExecutor delegate;

    @SuppressFBWarnings("EI2")
    public ToolCallLoggingSubagentExecutor(SubagentExecutor delegate, Context context) {
        this.context = context;
        this.delegate = delegate;
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagentDefinition) {
        Map<String, String> inputs = Map.of("prompt", taskCall.prompt());

        String result;

        try {
            result = delegate.execute(taskCall, subagentDefinition);
        } catch (RuntimeException exception) {
            ToolCallLoggingToolCallback.logFailure(context, subagentDefinition.getName(), inputs, exception);

            throw exception;
        }

        if (Boolean.TRUE.equals(taskCall.run_in_background())) {
            ToolCallLoggingToolCallback.logCall(context, subagentDefinition.getName(), inputs, result);
        }

        return result;
    }

    @Override
    public String getKind() {
        return delegate.getKind();
    }
}
