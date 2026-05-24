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

package com.bytechef.component.ai.agent.utils.util;

import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsUtils {

    private static final Pattern PERFORM_FUNCTION_PATTERN = Pattern.compile(
        "(?:function\\s+perform\\s*\\(|def\\s+perform\\s*[\\s(]|perform\\s*<-\\s*function\\s*\\(|\\bperform\\s*\\(Map)");
    private static final String SCRIPTS_DIRECTORY = "scripts/";

    public static boolean isScriptEntry(String entryName) {
        String normalized = entryName.replace('\\', '/');

        return normalized.startsWith(SCRIPTS_DIRECTORY) || normalized.contains("/" + SCRIPTS_DIRECTORY);
    }

    public static boolean hasPerformFunction(String content) {
        return PERFORM_FUNCTION_PATTERN.matcher(content)
            .find();
    }
}
