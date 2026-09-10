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

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.domain.Property;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ToolPropertyUtils {

    private static final int TOOL_NAME_MAX_LENGTH = 64;

    private static final String TOOL_NAME_DISALLOWED_CHARACTERS_REGEX = "[^A-Za-z0-9_./-]";

    public static Property toolNameProperty(String defaultSourceName) {
        return Property.toProperty(
            string(ToolConstants.TOOL_NAME)
                .label("Tool Name")
                .description(
                    "The tool name exposed to the AI model. Defaults to the " + defaultSourceName +
                        " name when left blank. Up to " + TOOL_NAME_MAX_LENGTH +
                        " characters, limited to letters, digits, '_', '-', '.' and '/'.")
                .placeholder("Defaults to " + defaultSourceName + " name")
                .expressionEnabled(false)
                .maxLength(TOOL_NAME_MAX_LENGTH)
                .regex(TOOL_NAME_DISALLOWED_CHARACTERS_REGEX)
                .required(false));
    }

    public static Property toolDescriptionProperty(String defaultSourceName) {
        return Property.toProperty(
            string(ToolConstants.TOOL_DESCRIPTION)
                .label("Tool Description")
                .description(
                    "The tool description exposed to the AI model. Defaults to the " + defaultSourceName +
                        " description when left blank.")
                .placeholder("Defaults to " + defaultSourceName + " description")
                .controlType(ControlType.TEXT_AREA)
                .expressionEnabled(false)
                .required(false));
    }

    public static boolean isToolOverrideProperty(Property property) {
        String name = property.getName();

        return ToolConstants.TOOL_NAME.equals(name) || ToolConstants.TOOL_DESCRIPTION.equals(name);
    }

    public static List<? extends Property> withoutToolOverrideProperties(List<? extends Property> properties) {
        return properties.stream()
            .filter(property -> !isToolOverrideProperty(property))
            .toList();
    }
}
