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
 * The approval-channel registry: one {@link ApprovalChannelDefinition} per component that installs an
 * {@code APPROVAL_CHANNELS}-type cluster element (see {@code ApprovalChannelFunction.APPROVAL_CHANNELS} in
 * {@code sdks/backend/java/component-api}). Consumed by {@code AiAgentWorkflowGenerator}, which asks each approval
 * destination whether it needs a {@code connections} block when nesting it under an
 * {@code aiAgentUtils/v1/approvalGateTool} (or an {@code approval/v1/requestApproval}) cluster element.
 * <p>
 * An agent reaches only the subset of these components that {@code ChannelDefinitions.approvalDelivery} maps its own
 * channel types onto — approval destinations are derived from the agent's channels rather than configured, so the
 * remaining entries below are unreachable from an agent today. They are kept because the registry is a catalogue of
 * what each component actually exposes: {@code defaultElementName}/{@code elementNames} record that, and a channel type
 * mapped onto one of them later needs no new research.
 * <p>
 * Every entry below was read from the corresponding component's cluster-element source rather than guessed — kept in
 * sync against a {@code find server -iname "*ApprovalChannel*.java"} sweep (13 components, 15 elements: every component
 * below has exactly one approval-channel element EXCEPT {@code twilio} and {@code infobip}, which each expose two,
 * {@code sms} and {@code whatsApp}). {@code connectionRequired} is {@code true} whenever the channel's {@code perform}
 * method reads {@code connectionParameters} and/or issues an HTTP call/SMTP send through the component's own
 * connection, and {@code false} for the two channels that need no connection at all ({@code chat}, which publishes onto
 * the run's own job SSE stream, and {@code approvalTask}, which only writes a local {@code ApprovalTask} row).
 *
 * @author Ivica Cardic
 */
public final class ApprovalChannelDefinitions {

    /**
     * One entry in the approval-channel registry.
     *
     * @param componentName      the component that owns the approval-channel cluster element
     * @param connectionRequired whether the generated cluster element carries a {@code connections} block
     * @param defaultElementName the cluster element name used when the stored row has no explicit {@code elementName}
     *                           parameter
     * @param elementNames       every valid {@code elementName} for this component — {@code slack} has no
     *                           {@code whatsApp} element, so a {@code slack}/{@code whatsApp} pair would name the
     *                           nonexistent {@code slack/v1/whatsApp}
     */
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

    /**
     * @throws IllegalArgumentException if {@code componentName} is not a known approval-channel component
     */
    public static ApprovalChannelDefinition getApprovalChannelDefinition(String componentName) {
        ApprovalChannelDefinition definition = APPROVAL_CHANNEL_DEFINITIONS_BY_COMPONENT_NAME.get(componentName);

        if (definition == null) {
            throw new IllegalArgumentException("Unknown approval channel component: " + componentName);
        }

        return definition;
    }

    private static Map<String, ApprovalChannelDefinition> buildDefinitions() {
        Map<String, ApprovalChannelDefinition> definitions = new LinkedHashMap<>();

        // Source: server/libs/modules/components/chat/.../cluster/ChatApprovalChannel.java —
        // clusterElement("chat"), perform(...) drops connectionParameters entirely and publishes onto the run's own
        // job SSE stream via a MessageBroker. connectionRequired=false.
        put(definitions, "chat", false, "chat");

        // Source: server/libs/modules/components/slack/.../cluster/SlackApprovalChannel.java —
        // clusterElement("slack"), reads connectionParameters(SIGNING_SECRET) and sends through the Slack API.
        put(definitions, "slack", true, "slack");

        // Source: server/libs/modules/components/discord/.../cluster/DiscordApprovalChannel.java —
        // clusterElement("discord"), reads connectionParameters(PUBLIC_KEY) and posts via context.http(...).
        put(definitions, "discord", true, "discord");

        // Source: server/libs/modules/components/telegram/.../cluster/TelegramApprovalChannel.java —
        // clusterElement("telegram"), reads connectionParameters(WEBHOOK_SECRET_TOKEN) and posts via context.http(...).
        put(definitions, "telegram", true, "telegram");

        // Source: server/libs/modules/components/mattermost/.../cluster/MattermostApprovalChannel.java —
        // clusterElement("mattermost"), posts via context.http(...) against the connected Mattermost server.
        put(definitions, "mattermost", true, "mattermost");

        // Source: server/libs/modules/components/rocketchat/.../cluster/RocketchatApprovalChannel.java —
        // clusterElement("rocketchat"), perform(...) delegates to RocketchatUtils.sendMessage(roomId, text, context),
        // which issues the HTTP call against the connected Rocket.Chat server (no direct connectionParameters read in
        // this class, but the delegate still requires the component's connection).
        put(definitions, "rocketchat", true, "rocketchat");

        // Source: server/libs/modules/components/email/.../cluster/EmailApprovalChannel.java —
        // clusterElement("email"), builds a Session from connectionParameters (host/username/password) and sends via
        // SMTP Transport.
        put(definitions, "email", true, "email");

        // Source: server/libs/modules/components/whatsapp/.../cluster/WhatsAppApprovalChannel.java —
        // clusterElement("whatsApp"), reads connectionParameters(APP_SECRET, PHONE_NUMBER_ID) and posts via
        // context.http(...) against the WhatsApp Cloud API. Component name "whatsApp"
        // (WhatsAppComponentHandler.component("whatsApp")).
        put(definitions, "whatsApp", true, "whatsApp");

        // Source: server/libs/modules/components/twilio/.../cluster/TwilioSmsApprovalChannel.java
        // (clusterElement("sms")) and TwilioWhatsAppApprovalChannel.java (clusterElement("whatsApp")) — both read
        // connectionParameters(USERNAME) and post via context.http(...) against the Twilio REST API.
        // TwilioComponentHandler.component("twilio"); "sms" is the default of the two elements.
        put(definitions, "twilio", true, "sms", Set.of("sms", "whatsApp"));

        // Source: server/libs/modules/components/infobip/.../cluster/InfobipSmsApprovalChannel.java
        // (clusterElement("sms")) and InfobipWhatsAppApprovalChannel.java (clusterElement("whatsApp")) — both post via
        // context.http(...) against the connected Infobip API. InfobipComponentHandler.component("infobip"); "sms" is
        // the default of the two elements.
        put(definitions, "infobip", true, "sms", Set.of("sms", "whatsApp"));

        // Source: server/libs/modules/components/approval-task/.../cluster/ApprovalTaskApprovalChannel.java —
        // clusterElement("approvalTask"), perform(...) drops connectionParameters entirely and only writes a local
        // ApprovalTask row via ApprovalTaskFacade. connectionRequired=false.
        put(definitions, "approvalTask", false, "approvalTask");

        // Source: server/libs/modules/components/google/google-mail/.../cluster/GoogleMailApprovalChannel.java —
        // clusterElement("googleMail"), resolves a Gmail client from connectionParameters via
        // GoogleServices.getMail(...) and sends through the Gmail API. Component name "googleMail"
        // (GoogleMailComponentHandler.component("googleMail")).
        put(definitions, "googleMail", true, "googleMail");

        // Source: server/libs/modules/components/microsoft/microsoft-outlook-365/.../cluster/
        // MicrosoftOutlook365ApprovalChannel.java — clusterElement("microsoftOutlook365"), posts via
        // context.http(...) against the connected Microsoft Graph /me/sendMail endpoint. Component name
        // "microsoftOutlook365" (MicrosoftOutlook365ComponentHandler.component("microsoftOutlook365")).
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
