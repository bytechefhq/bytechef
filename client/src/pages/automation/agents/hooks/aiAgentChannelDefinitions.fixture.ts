import {type AiAgentChannelDefinitionType} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';

/**
 * What `aiAgentChannelDefinitions` returns against the components this repo ships, transcribed from their
 * `agentChannel(...)` declarations plus the resolver's synthesized `schedule` entry.
 *
 * Shared by every test that mocks `useAiAgentChannelDefinitions` rather than copied per file: the maps these
 * definitions replaced were duplicated across three components and drifted, and three hand-copied fixtures would
 * reintroduce the same failure mode one layer down.
 *
 * Order matches the resolver's: registry order, with the synthesized `schedule` entry last.
 */
export const AI_AGENT_CHANNEL_DEFINITIONS: AiAgentChannelDefinitionType[] = [
    {
        approvalCapable: true,
        channelType: 'chat',
        componentName: 'chat',
        componentVersion: 1,
        connectionRequired: false,
        description: null,
        icon: null,
        pinned: true,
        // false because the chat channel PINS the trigger's only property (mode = hosted), so nothing is
        // left for a user to set. Not because the trigger has no properties.
        propertiesConfigurable: false,
        replyActionName: 'responseToRequest',
        schedule: false,
        title: 'Chat',
        triggerName: 'newChatRequest',
    },
    {
        approvalCapable: false,
        channelType: 'workflowCall',
        componentName: 'workflow',
        componentVersion: 1,
        connectionRequired: false,
        description: null,
        icon: null,
        pinned: true,
        propertiesConfigurable: false,
        replyActionName: 'responseToWorkflowCall',
        schedule: false,
        title: 'Workflow Call',
        triggerName: 'newWorkflowCall',
    },
    {
        approvalCapable: true,
        channelType: 'slack',
        componentName: 'slack',
        componentVersion: 1,
        connectionRequired: true,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: false,
        replyActionName: 'sendChannelMessage',
        schedule: false,
        title: 'Slack',
        triggerName: 'newMessage',
    },
    {
        approvalCapable: true,
        channelType: 'telegram',
        componentName: 'telegram',
        componentVersion: 1,
        connectionRequired: true,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: false,
        replyActionName: 'sendMessage',
        schedule: false,
        title: 'Telegram',
        triggerName: 'newMessage',
    },
    {
        approvalCapable: true,
        channelType: 'rocketchat',
        componentName: 'rocketchat',
        componentVersion: 1,
        connectionRequired: true,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: false,
        replyActionName: 'sendChannelMessage',
        schedule: false,
        title: 'Rocket.Chat',
        triggerName: 'newMessage',
    },
    // The two WhatsApp-carrying channels. Each component also exposes SMS and inbound-call operations, so the
    // declarations disambiguate their titles server-side rather than leaving the client to spell "Twilio".
    {
        approvalCapable: true,
        channelType: 'twilio',
        componentName: 'twilio',
        componentVersion: 1,
        connectionRequired: true,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: true,
        replyActionName: 'sendWhatsAppMessage',
        schedule: false,
        title: 'Twilio (WhatsApp)',
        triggerName: 'newWhatsappMessage',
    },
    {
        approvalCapable: true,
        channelType: 'infobip',
        componentName: 'infobip',
        componentVersion: 1,
        connectionRequired: true,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: true,
        replyActionName: 'sendWhatsappTextMessage',
        schedule: false,
        title: 'Infobip (WhatsApp)',
        triggerName: 'newWhatsappMessage',
    },
    // Declared server-side like any other channel — the hook, not the query, is what keeps it off the add menu.
    // Its component name capitalizes the "A" while its channel type does not, which is exactly the kind of
    // detail the client used to carry by hand.
    {
        approvalCapable: true,
        channelType: 'whatsapp',
        componentName: 'whatsApp',
        componentVersion: 1,
        connectionRequired: true,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: true,
        replyActionName: 'sendMessage',
        schedule: false,
        title: 'WhatsApp',
        triggerName: 'messageReceived',
    },
    {
        approvalCapable: false,
        channelType: 'schedule',
        componentName: 'schedule',
        componentVersion: 1,
        connectionRequired: false,
        description: null,
        icon: null,
        pinned: false,
        propertiesConfigurable: true,
        replyActionName: null,
        schedule: true,
        title: 'Schedule',
        triggerName: 'cron',
    },
];

/**
 * A stand-in for `useAiAgentChannelDefinitionsQuery`, so tests mock the GENERATED QUERY and let the real
 * `useAiAgentChannelDefinitions` run on top of it. Mocking the hook itself would make its own filtering — which
 * is where whatsApp is kept off the add menu — untested at every call site that depends on it.
 */
export const aiAgentChannelDefinitionsQueryResult = (
    definitions: AiAgentChannelDefinitionType[] = AI_AGENT_CHANNEL_DEFINITIONS
) => ({
    data: {aiAgentChannelDefinitions: definitions},
    isLoading: false,
});
