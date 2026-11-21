import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AgentChannelIcon from '@/pages/automation/agents/components/AgentChannelIcon';
import {
    type AiAgentChannelDefinitionType,
    useAiAgentChannelDefinitions,
} from '@/pages/automation/agents/hooks/useAiAgentChannelDefinitions';
import {describeCronExpression} from '@/pages/automation/agents/utils/agentScheduleCron';
import isScheduleTrigger from '@/pages/automation/project-deployments/components/agent-deployment-channel-list/isScheduleTrigger';
import {getPageUrl} from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/util/pageUrl-utils';
import {getAgentChatApi} from '@/shared/edition/agent-chat/agentChatApi';
import {AiAgentDeploymentTrigger, AiAgentDeploymentWorkflow} from '@/shared/middleware/graphql';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {CalendarClockIcon, ClipboardIcon, MessageCircleIcon} from 'lucide-react';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';

const CHAT_CHANNEL_TYPE = 'chat';

export const WORKFLOW_CALL_TRIGGER_TYPE = 'workflow/v1/newWorkflowCall';

const findChannelDefinition = (definitions: AiAgentChannelDefinitionType[], triggerType: string) => {
    const [componentName, , triggerName] = triggerType.split('/');

    return (
        definitions.find(
            (definition) => definition.componentName === componentName && definition.triggerName === triggerName
        ) ?? definitions.find((definition) => definition.componentName === componentName)
    );
};

interface AgentDeploymentChannelListItemProps {
    definition?: AiAgentChannelDefinitionType;
    projectDeploymentId: string;
    title: string;
    trigger: AiAgentDeploymentTrigger;
}

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

    const isMessagingTrigger = definition != null && !definition.pinned && !definition.schedule;

    const staticWebhookUrl = trigger.staticWebhookUrl;

    const openAgentChat = getAgentChatApi().useOpenAgentChat();

    return (
        <span className="flex shrink-0 items-center gap-1 rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary py-0.5 pr-0.5 pl-2 text-xs">
            <AgentChannelIcon channelType={definition?.channelType} className="mr-0.5" icon={definition?.icon} />

            <span className="truncate">{label}</span>

            <span className="flex size-6 flex-none items-center justify-center">
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
                                        workflowExecutionId: staticWebhookUrl.substring(
                                            staticWebhookUrl.lastIndexOf('/webhooks/') + '/webhooks/'.length
                                        ),
                                    })
                                }
                                size="iconXs"
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
                                className="flex size-6 items-center justify-center rounded-md p-1 text-content-neutral-secondary hover:bg-surface-neutral-primary-hover hover:text-content-brand-primary"
                                to={getPageUrl('chats', undefined, staticWebhookUrl)}
                            >
                                <MessageCircleIcon className="size-3.5" />
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
                        size="iconXs"
                        variant="ghost"
                    />
                )}
            </span>
        </span>
    );
};

interface AgentDeploymentChannelListProps {
    projectDeploymentId: string;
    title: string;
    workflows: AiAgentDeploymentWorkflow[];
}

const AgentDeploymentChannelList = ({projectDeploymentId, title, workflows}: AgentDeploymentChannelListProps) => {
    const {definitions} = useAiAgentChannelDefinitions();

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
        return <span className="text-xs text-muted-foreground">No triggers configured for this deployment.</span>;
    }

    const channelRows = rows.filter(({trigger}) => !isScheduleTrigger(trigger));

    if (!channelRows.length) {
        return null;
    }

    return (
        <div className="flex min-w-0 flex-wrap items-center gap-2">
            {channelRows.map(({definition, key, trigger}) => (
                <AgentDeploymentChannelListItem
                    definition={definition}
                    key={key}
                    projectDeploymentId={projectDeploymentId}
                    title={title}
                    trigger={trigger}
                />
            ))}
        </div>
    );
};

interface AgentDeploymentScheduleProps {
    workflows: AiAgentDeploymentWorkflow[];
}

export const AgentDeploymentSchedule = ({workflows}: AgentDeploymentScheduleProps) => {
    const scheduleSummary = useMemo(
        () =>
            workflows
                .flatMap((workflow) => workflow.triggers)
                .filter((trigger) => isScheduleTrigger(trigger))
                .map(
                    (trigger) =>
                        describeCronExpression((trigger.parameters as {expression?: string} | undefined)?.expression) ||
                        'Runs on schedule'
                )
                .join(', '),
        [workflows]
    );

    if (!scheduleSummary) {
        return null;
    }

    return (
        <span className="mr-6 hidden min-w-0 items-center gap-1 truncate text-sm font-normal text-content-neutral-secondary sm:flex">
            <CalendarClockIcon className="size-4 shrink-0" />

            <span className="truncate">{scheduleSummary}</span>
        </span>
    );
};

export default AgentDeploymentChannelList;
