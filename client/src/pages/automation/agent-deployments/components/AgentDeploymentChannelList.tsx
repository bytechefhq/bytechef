import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    type AiAgentChannelDefinitionType,
    useAiAgentChannelDefinitions,
} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';
import {getPageUrl} from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/util/pageUrl-utils';
import {getAgentChatApi} from '@/shared/edition/agent-chat/agentChatApi';
import {AiAgentDeploymentTrigger, AiAgentDeploymentWorkflow} from '@/shared/middleware/graphql';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, MessageCircleIcon} from 'lucide-react';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';

// The one reserved channel key this list names, matching AiAgentChannelType.CHAT server-side.
const CHAT_CHANNEL_TYPE = 'chat';

export const WORKFLOW_CALL_TRIGGER_TYPE = 'workflow/v1/newWorkflowCall';

/**
 * A deployment carries a trigger TYPE (`<component>/v<version>/<trigger>`) where the registry is keyed by channel
 * type, so the channel is recovered from the two the definition also carries.
 *
 * The fallback to the component alone is what keeps an already-published deployment readable: its trigger type is
 * frozen at publish time, so a workflow published before Slack's channel moved from `anyEvent` to `newMessage`
 * still has to name its channel rather than print a bare lowercase component name.
 */
const findChannelDefinition = (definitions: AiAgentChannelDefinitionType[], triggerType: string) => {
    const [componentName, , triggerName] = triggerType.split('/');

    return (
        definitions.find(
            (definition) => definition.componentName === componentName && definition.triggerName === triggerName
        ) ?? definitions.find((definition) => definition.componentName === componentName)
    );
};

interface AgentDeploymentChannelListItemProps {
    /** The channel this trigger belongs to, or undefined when no deployed component declares it. */
    definition?: AiAgentChannelDefinitionType;
    /** The ProjectDeployment id and agent title, needed to open the chat as a conversation on EE. */
    projectDeploymentId: string;
    title: string;
    trigger: AiAgentDeploymentTrigger;
}

// Each trigger carries its OWN staticWebhookUrl (per-trigger, resolved server-side) — a workflow with both a slack
// and a telegram channel must never show one channel's URL under the other's row. Telegram is DYNAMIC_WEBHOOK (it
// registers its webhook with the provider itself), so its staticWebhookUrl is always null and it correctly gets no
// copy button below, same as schedule/workflowCall.
const AgentDeploymentChannelListItem = ({
    definition,
    projectDeploymentId,
    title,
    trigger,
}: AgentDeploymentChannelListItemProps) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const label = definition?.title || trigger.type.split('/')[0];

    const isChatTrigger = definition?.channelType === CHAT_CHANNEL_TYPE;
    const isScheduleTrigger = definition?.schedule ?? false;

    // Every channel that is neither pinned nor the schedule is a messaging channel reached over a STATIC_WEBHOOK,
    // so it gets the copy-url affordance -- derived rather than listed, which is what brings twilio and infobip in
    // (a hand-written prefix list had simply never been extended to them).
    const isMessagingTrigger = definition != null && !definition.pinned && !definition.schedule;

    const expression = (trigger.parameters as {expression?: string} | undefined)?.expression;
    const staticWebhookUrl = trigger.staticWebhookUrl;

    // Null on CE, where there is no AI Hub to open a conversation in — the hosted chat page is the destination
    // there, and remains the one a shared link points at.
    const openAgentChat = getAgentChatApi().useOpenAgentChat();

    return (
        <li className="flex items-center justify-between gap-4 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <span className="text-sm font-medium">{label}</span>

            {/* Fixed-width cell so each row's control lands on the same centre line as the menu button in the
                header above, which is an icon-sized button inset by the same px-3. */}

            {isScheduleTrigger && (
                <span className="ml-auto text-xs text-muted-foreground">
                    {expression ? `Runs on schedule: ${expression}` : 'Runs on schedule'}
                </span>
            )}

            {/* Fixed-width cell so each row's control lands on the same centre line as the menu button in the
                header above, which is an icon-sized button inset by the same px-3. */}

            <div className="flex w-9 flex-none justify-center">
                {isChatTrigger && staticWebhookUrl && openAgentChat && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label="Open chat"
                                icon={<MessageCircleIcon />}
                                onClick={() =>
                                    openAgentChat({
                                        projectDeploymentId,
                                        title,
                                        // The same segment getPageUrl reads to build the hosted chat URL: a
                                        // workflow chat is keyed on the trigger's webhook execution id.
                                        workflowExecutionId: staticWebhookUrl.substring(
                                            staticWebhookUrl.lastIndexOf('/webhooks/') + '/webhooks/'.length
                                        ),
                                    })
                                }
                                size="iconSm"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Open chat</TooltipContent>
                    </Tooltip>
                )}

                {isChatTrigger && staticWebhookUrl && !openAgentChat && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Link
                                aria-label="Open hosted chat"
                                // size-8 p-2 is the iconSm Button footprint the sibling rows' copy button uses;
                                // a bare icon here made the chat row visibly shorter than the rest of the list.
                                className="flex size-8 items-center justify-center rounded-md p-2 text-content-neutral-secondary hover:bg-surface-neutral-primary-hover hover:text-content-brand-primary"
                                to={getPageUrl('chats', undefined, staticWebhookUrl)}
                            >
                                <MessageCircleIcon className="size-4" />
                            </Link>
                        </TooltipTrigger>

                        <TooltipContent>Open hosted chat</TooltipContent>
                    </Tooltip>
                )}

                {isMessagingTrigger && staticWebhookUrl && (
                    <Button
                        aria-label={`Copy ${label} webhook URL`}
                        icon={<ClipboardIcon />}
                        onClick={() => copyToClipboard(staticWebhookUrl)}
                        size="iconSm"
                        variant="ghost"
                    />
                )}
            </div>
        </li>
    );
};

interface AgentDeploymentChannelListProps {
    projectDeploymentId: string;
    title: string;
    workflows: AiAgentDeploymentWorkflow[];
}

const AgentDeploymentChannelList = ({projectDeploymentId, title, workflows}: AgentDeploymentChannelListProps) => {
    const {definitions} = useAiAgentChannelDefinitions();

    // workflowCall is hidden for the same reason it is hidden on the agent detail page: the generator always
    // emits it, it carries no configuration and nothing here can act on it, so the row was permanently inert.
    const rows = useMemo(
        () =>
            workflows.flatMap((workflow) =>
                workflow.triggers
                    .filter((trigger) => trigger.type !== WORKFLOW_CALL_TRIGGER_TYPE)
                    .map((trigger) => ({
                        definition: findChannelDefinition(definitions, trigger.type),
                        key: `${workflow.workflowId}-${trigger.name}`,
                        trigger,
                    }))
            ),
        [definitions, workflows]
    );

    if (!rows.length) {
        return <p className="px-3 py-2 text-sm text-muted-foreground">No triggers configured for this deployment.</p>;
    }

    return (
        <ul className="divide-y divide-stroke-neutral-primary">
            {rows.map(({definition, key, trigger}) => (
                <AgentDeploymentChannelListItem
                    definition={definition}
                    key={key}
                    projectDeploymentId={projectDeploymentId}
                    title={title}
                    trigger={trigger}
                />
            ))}
        </ul>
    );
};

export default AgentDeploymentChannelList;
