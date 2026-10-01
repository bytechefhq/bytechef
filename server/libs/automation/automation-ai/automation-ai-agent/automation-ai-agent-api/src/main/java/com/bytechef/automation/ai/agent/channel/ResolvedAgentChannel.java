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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ResolvedAgentChannel(
    String name, String title, @Nullable String description, @Nullable String icon, String triggerType,
    @Nullable String replyActionType, boolean connectionRequired, Map<String, Object> triggerParameters,
    Map<String, Object> triggerPropertyDefaults, Set<String> triggerPropertyNames,
    Set<String> requiredReplyPropertyNames, Binding binding, @Nullable ApprovalDelivery approvalDelivery) {

    public ResolvedAgentChannel {
        triggerParameters = orderedCopyOf(triggerParameters);
        triggerPropertyDefaults = orderedCopyOf(triggerPropertyDefaults);
        triggerPropertyNames = Collections.unmodifiableSet(new LinkedHashSet<>(triggerPropertyNames));
        requiredReplyPropertyNames = Collections.unmodifiableSet(new LinkedHashSet<>(requiredReplyPropertyNames));
    }

    @SuppressFBWarnings("EI")
    public record Binding(
        @Nullable String conversationIdPath, @Nullable String messagePath, @Nullable String attachmentsPath,
        @Nullable String replyMessageProperty, @Nullable String replyConversationIdProperty,
        @Nullable String replyAttachmentsProperty, Map<String, String> replyChannelParameters,
        Map<String, Object> replyFixedParameters) {

        public Binding {
            replyChannelParameters = orderedCopyOf(replyChannelParameters);
            replyFixedParameters = orderedCopyOf(replyFixedParameters);
        }
    }

    public record ApprovalDelivery(String componentName, String elementName) {
    }

    public String componentName() {
        return triggerType.substring(0, triggerType.indexOf('/'));
    }

    public boolean propertiesConfigurable() {
        return triggerPropertyNames.stream()
            .anyMatch(propertyName -> !triggerParameters.containsKey(propertyName));
    }

    private static <V> Map<String, V> orderedCopyOf(Map<String, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}
