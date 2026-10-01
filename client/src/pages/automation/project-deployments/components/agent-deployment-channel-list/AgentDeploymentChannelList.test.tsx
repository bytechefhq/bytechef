import {TooltipProvider} from '@/components/ui/tooltip';
import {aiAgentChannelDefinitionsQueryResult} from '@/pages/automation/agents/hooks/aiAgentChannelDefinitions.fixture';
import {type AiAgentDeploymentWorkflow} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AgentDeploymentChannelList, {AgentDeploymentSchedule} from './AgentDeploymentChannelList';

const copyToClipboard = vi.fn();

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [undefined, copyToClipboard],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentChannelDefinitionsQuery: () => aiAgentChannelDefinitionsQueryResult(),
}));

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

    it('states a picker-written schedule in words rather than as cron', () => {
        const scheduledWorkflow = {
            ...workflow,
            triggers: workflow.triggers.map((trigger) =>
                trigger.name === 'trigger_schedule'
                    ? {...trigger, parameters: {expression: '57 7 * * ?', timezone: 'UTC'}}
                    : trigger
            ),
        };

        wrap(<AgentDeploymentSchedule workflows={[scheduledWorkflow]} />);

        expect(screen.getByText('Daily at 07:57')).toBeInTheDocument();
        expect(screen.queryByText('57 7 * * ?')).not.toBeInTheDocument();
    });

    it('falls back to the expression of a schedule the picker could not have written', () => {
        wrap(<AgentDeploymentSchedule workflows={[workflow]} />);

        expect(screen.getByText('0 9 * * *')).toBeInTheDocument();
    });

    it('leaves the schedule out of the channel chips', () => {
        wrap(<AgentDeploymentChannelList projectDeploymentId="20" title="Agent One" workflows={[workflow]} />);

        expect(screen.queryByText('0 9 * * *')).not.toBeInTheDocument();
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
