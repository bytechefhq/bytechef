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

package com.bytechef.platform.ai.tool.util;

import static com.bytechef.platform.ai.tool.util.ToolPropertyUtils.TOOL_NAME_MAX_LENGTH;

import com.bytechef.platform.ai.tool.constant.ToolConstants;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class ToolNameUtils {

    private static final Pattern EDGE_NON_TOOL_NAME_CHARACTERS = Pattern.compile("^[^a-zA-Z0-9_-]+|[^a-zA-Z0-9_-]+$");

    private static final Pattern NON_TOOL_NAME_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_-]+");

    private static final String TOOL_NAME_LETTERS_AND_DIGITS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private ToolNameUtils() {
    }

    public static String getDefaultToolName(String componentName, String clusterElementName) {
        StringBuilder sb = new StringBuilder();

        sb.append(componentName.toUpperCase(Locale.ROOT));
        sb.append("_");
        sb.append(Character.toUpperCase(clusterElementName.charAt(0)));

        for (int i = 1; i < clusterElementName.length(); i++) {
            char character = clusterElementName.charAt(i);

            if (Character.isUpperCase(character)) {
                sb.append('_')
                    .append(character);
            } else {
                sb.append(Character.toUpperCase(character));
            }
        }

        return sb.toString();
    }

    public static @Nullable String getCustomToolName(@Nullable Map<String, ?> parameters) {
        if (parameters == null || !(parameters.get(ToolConstants.TOOL_NAME) instanceof String name)) {
            return null;
        }

        String toolName = EDGE_NON_TOOL_NAME_CHARACTERS.matcher(name)
            .replaceAll("");

        toolName = NON_TOOL_NAME_CHARACTERS.matcher(toolName)
            .replaceAll("_");

        toolName = StringUtils.left(toolName, TOOL_NAME_MAX_LENGTH);

        return StringUtils.containsAny(toolName, TOOL_NAME_LETTERS_AND_DIGITS) ? toolName : null;
    }

    public static @Nullable String getWorkflowToolName(@Nullable Map<String, ?> parameters, @Nullable String label) {
        String toolName = getCustomToolName(parameters);

        if (toolName != null) {
            return toolName;
        }

        return toToolName(label);
    }

    private static @Nullable String toToolName(@Nullable String label) {
        if (label == null) {
            return null;
        }

        String toolName = NON_TOOL_NAME_CHARACTERS.matcher(label.trim())
            .replaceAll("_");

        toolName = StringUtils.strip(toolName, "_");
        toolName = StringUtils.left(toolName, TOOL_NAME_MAX_LENGTH);
        toolName = StringUtils.stripEnd(toolName, "_");

        return toolName.isBlank() ? null : toolName.toUpperCase(Locale.ROOT);
    }
}
