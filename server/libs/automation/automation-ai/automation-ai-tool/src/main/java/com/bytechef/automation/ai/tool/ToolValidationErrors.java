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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class ToolValidationErrors {

    private ToolValidationErrors() {
    }

    public static String validationFailure(Class<?> sourceClass, String toolName, Exception exception) {
        return ToolErrors.toolError(resolveMessage(sourceClass, toolName, exception));
    }

    private static String resolveMessage(Class<?> sourceClass, String toolName, Exception exception) {
        Logger sourceLogger = LoggerFactory.getLogger(sourceClass);

        sourceLogger.warn("{} could not complete the request", toolName, exception);

        String message = exception.getMessage();

        if (message != null && !message.isBlank()) {
            return message;
        }

        Class<? extends Exception> exceptionClass = exception.getClass();

        return toolName + " could not complete the request (" + exceptionClass.getSimpleName() + ")";
    }
}
