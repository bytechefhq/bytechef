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

package com.bytechef.automation.ai.agent.channel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public final class ApprovalChannelDefinitions {

    public record ApprovalChannelDefinition(
        String componentName, boolean connectionRequired, String defaultElementName, Set<String> elementNames) {

        public ApprovalChannelDefinition {
            elementNames = Set.copyOf(elementNames);
        }
    }

    private static final Map<String, ApprovalChannelDefinition> APPROVAL_CHANNEL_DEFINITIONS_BY_COMPONENT_NAME =
        buildDefinitions();

    private ApprovalChannelDefinitions() {
    }

    public static ApprovalChannelDefinition getApprovalChannelDefinition(String componentName) {
        ApprovalChannelDefinition definition = APPROVAL_CHANNEL_DEFINITIONS_BY_COMPONENT_NAME.get(componentName);

        if (definition == null) {
            throw new IllegalArgumentException("Unknown approval channel component: " + componentName);
        }

        return definition;
    }

    private static Map<String, ApprovalChannelDefinition> buildDefinitions() {
        Map<String, ApprovalChannelDefinition> definitions = new LinkedHashMap<>();

        put(definitions, "chat", false, "chat");

        put(definitions, "slack", true, "slack");

        put(definitions, "discord", true, "discord");

        put(definitions, "telegram", true, "telegram");

        put(definitions, "mattermost", true, "mattermost");

        put(definitions, "rocketchat", true, "rocketchat");

        put(definitions, "email", true, "email");

        put(definitions, "whatsApp", true, "whatsApp");

        put(definitions, "twilio", true, "sms", Set.of("sms", "whatsApp"));

        put(definitions, "infobip", true, "sms", Set.of("sms", "whatsApp"));

        put(definitions, "approvalTask", false, "approvalTask");

        put(definitions, "googleMail", true, "googleMail");

        put(definitions, "microsoftOutlook365", true, "microsoftOutlook365");

        return Map.copyOf(definitions);
    }

    private static void put(
        Map<String, ApprovalChannelDefinition> definitions, String componentName, boolean connectionRequired,
        String defaultElementName) {

        put(definitions, componentName, connectionRequired, defaultElementName, Set.of(defaultElementName));
    }

    private static void put(
        Map<String, ApprovalChannelDefinition> definitions, String componentName, boolean connectionRequired,
        String defaultElementName, Set<String> elementNames) {

        definitions.put(
            componentName,
            new ApprovalChannelDefinition(componentName, connectionRequired, defaultElementName, elementNames));
    }
}
