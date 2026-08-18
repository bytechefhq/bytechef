import {TooltipProvider} from '@/components/ui/tooltip';
import {aiAgentChannelDefinitionsQueryResult} from '@/pages/automation/agents/hooks/aiAgentChannelDefinitions.fixture';
import {type AiAgentDeploymentWorkflow} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AgentDeploymentChannelList from './AgentDeploymentChannelList';

const copyToClipboard = vi.fn();

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [undefined, copyToClipboard],
}));

// Only the generated query is stubbed; the real useAiAgentChannelDefinitions runs, and this list's own
// trigger-type-to-channel mapping runs on top of it.
vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentChannelDefinitionsQuery: () => aiAgentChannelDefinitionsQueryResult(),
}));

// TooltipProvider: the chat row's link is a tooltip trigger (the label became an icon), and Radix throws
// outside a provider. main.tsx mounts one globally, so this only stands in for that.
const wrap = (ui: React.ReactNode) =>
    render(
        <MemoryRouter>
            <TooltipProvider>{ui}</TooltipProvider>
        </MemoryRouter>
    );

const chatStaticWebhookUrl = 'https://example.com/webhooks/chat-abc123';
const slackStaticWebhookUrl = 'https://example.com/webhooks/slack-def456';
const twilioStaticWebhookUrl = 'https://example.com/webhooks/twilio-ghi789';
const infobipStaticWebhookUrl = 'https://example.com/webhooks/infobip-jkl012';

// Each trigger carries its OWN staticWebhookUrl (server resolves it per-trigger, not once per workflow) — chat and
// slack are both STATIC_WEBHOOK but get DIFFERENT URLs here, proving a fix for the bug where every messaging row
// showed the workflow's first (chat) trigger's URL. Telegram is DYNAMIC_WEBHOOK (self-registers with the provider),
// so its staticWebhookUrl is null and it must render no copy button at all.
const workflow: AiAgentDeploymentWorkflow = {
    __typename: 'AiAgentDeploymentWorkflow',
    enabled: true,
    triggers: [
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_chat',
            parameters: {},
            staticWebhookUrl: chatStaticWebhookUrl,
            type: 'chat/v1/newChatRequest',
        },
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_slack',
            parameters: {},
            staticWebhookUrl: slackStaticWebhookUrl,
            type: 'slack/v1/newMessage',
        },
        // Twilio and Infobip are WhatsApp-carrying messaging channels like any other. Both were simply missing
        // from the hand-written label map and the hand-written messaging-prefix list, so their rows printed a
        // bare lowercase component name and never offered the copy-webhook-url button.
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_twilio',
            parameters: {},
            staticWebhookUrl: twilioStaticWebhookUrl,
            type: 'twilio/v1/newWhatsappMessage',
        },
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_infobip',
            parameters: {},
            staticWebhookUrl: infobipStaticWebhookUrl,
            type: 'infobip/v1/newWhatsappMessage',
        },
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_telegram',
            parameters: {},
            staticWebhookUrl: null,
            type: 'telegram/v1/newMessage',
        },
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_workflow_call',
            parameters: {},
            staticWebhookUrl: null,
            type: 'workflow/v1/newWorkflowCall',
        },
        {
            __typename: 'AiAgentDeploymentTrigger',
            name: 'trigger_schedule',
            parameters: {expression: '0 9 * * *', timezone: 'UTC'},
            staticWebhookUrl: null,
            type: 'schedule/v1/cron',
        },
    ],
    workflowId: 'workflow-1',
};

describe('AgentDeploymentChannelList', () => {
    it('renders a chats link for the chat trigger', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        const link = screen.getByRole('link', {name: /open hosted chat/i});

        expect(link).toHaveAttribute('href', expect.stringContaining('/automation/chats/'));
    });

    it("renders a copy button for the slack trigger using its own webhook URL, not the chat trigger's", () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        const button = screen.getByRole('button', {name: /copy slack webhook url/i});

        button.click();

        expect(copyToClipboard).toHaveBeenCalledWith(slackStaticWebhookUrl);
    });

    // Both bugs the hand-written maps carried, in one test per channel: the label came out as the raw lowercase
    // component name ("twilio"/"infobip") because neither was in CHANNEL_LABELS, and no copy button rendered at
    // all because neither prefix was in MESSAGING_CHANNEL_PREFIXES.
    it('labels the twilio trigger and offers its webhook URL', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        expect(screen.getByText('Twilio (WhatsApp)')).toBeInTheDocument();
        expect(screen.queryByText('twilio')).not.toBeInTheDocument();

        screen.getByRole('button', {name: /copy twilio \(whatsapp\) webhook url/i}).click();

        expect(copyToClipboard).toHaveBeenCalledWith(twilioStaticWebhookUrl);
    });

    it('labels the infobip trigger and offers its webhook URL', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        expect(screen.getByText('Infobip (WhatsApp)')).toBeInTheDocument();
        expect(screen.queryByText('infobip')).not.toBeInTheDocument();

        screen.getByRole('button', {name: /copy infobip \(whatsapp\) webhook url/i}).click();

        expect(copyToClipboard).toHaveBeenCalledWith(infobipStaticWebhookUrl);
    });

    // A deployment's trigger type is frozen at publish time, so a workflow published while Slack's channel still
    // paired anyEvent has to keep naming its channel rather than printing a bare component name.
    it('names a channel whose deployed trigger predates a trigger rename', () => {
        wrap(
            <AgentDeploymentChannelList
                projectDeploymentId="20"
                title="Agent One"
                workflows={[
                    {
                        ...workflow,
                        triggers: [
                            {
                                __typename: 'AiAgentDeploymentTrigger',
                                name: 'trigger_slack',
                                parameters: {},
                                staticWebhookUrl: slackStaticWebhookUrl,
                                type: 'slack/v1/anyEvent',
                            },
                        ],
                    },
                ]}
            />
        );

        expect(screen.getByText('Slack')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /copy slack webhook url/i})).toBeInTheDocument();
    });

    it('renders no copy button for the telegram (DYNAMIC_WEBHOOK) trigger', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        expect(screen.queryByRole('button', {name: /copy telegram webhook url/i})).not.toBeInTheDocument();
    });

    it('omits the workflowCall trigger, which carries no configuration to show', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        expect(screen.queryByText('Workflow Call')).not.toBeInTheDocument();
    });

    it('renders the cron expression caption for the schedule trigger', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        expect(screen.getByText('Runs on schedule: 0 9 * * *')).toBeInTheDocument();
    });

    it('renders a fallback message when no triggers are configured', () => {
        wrap(
            <AgentDeploymentChannelList
                projectDeploymentId="20"
                title="Agent One"
                workflows={[{...workflow, triggers: []}]}
            />
        );

        expect(screen.getByText(/no triggers configured/i)).toBeInTheDocument();
    });
});
