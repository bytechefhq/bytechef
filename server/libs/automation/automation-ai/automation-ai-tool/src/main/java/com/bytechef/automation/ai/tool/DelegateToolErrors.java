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

package com.bytechef.automation.ai.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.exception.ExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionException;

/**
 * @author Ivica Cardic
 */
final class DelegateToolErrors {

    private DelegateToolErrors() {
    }

    static String toToolError(Class<?> sourceClass, String toolName, RuntimeException exception) {
        RuntimeException failure = exception;

        if (exception instanceof ToolExecutionException && exception.getCause() instanceof RuntimeException cause) {
            failure = cause;
        }

        String message = failure.getMessage();

        if (message != null && isToolError(failure)) {
            return ToolErrors.toolError(message);
        }

        return ToolErrors.runtimeFailure(sourceClass, toolName, failure);
    }

    private static boolean isToolError(RuntimeException exception) {
        return exception instanceof ExecutionException || exception instanceof ChatModelUnavailableException;
    }
}
