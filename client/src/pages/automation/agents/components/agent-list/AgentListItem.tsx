import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import AgentVisibilityCaveat from '@/pages/automation/agents/components/AgentVisibilityCaveat';
import exportAgent from '@/pages/automation/agents/utils/agentImportExport';
import {describeCadence, fromCadenceParameters} from '@/pages/automation/agents/utils/agentScheduleCron';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import isScheduledAgent from '@/pages/automation/agents/utils/isScheduledAgent';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker, {
    ResourceVisibilityValueType,
} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useAiAgentVisibility} from '@/shared/hooks/useAiAgentVisibility';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {
    AiAgent,
    useAiAgentTagsQuery,
    useDeleteAiAgentMutation,
    usePublishAiAgentMutation,
    useUpdateAiAgentTagsMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import isInteractiveElementClick from '@/shared/util/interactive-element-utils';
import {useQueryClient} from '@tanstack/react-query';
import {
    CalendarClockIcon,
    DownloadIcon,
    EllipsisVerticalIcon,
    PencilIcon,
    RocketIcon,
    SendIcon,
    Trash2Icon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface AgentListItemProps {
    agent: AiAgent;
}

const AgentListItem = ({agent}: AgentListItemProps) => {
    const [showDeployDialog, setShowDeployDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    // The same hook the detail page's dialog uses, so the badge dropdown and the dialog cannot drift on the
    // grant diff or on which caches they invalidate — the project list item and project header share
    // useProjectVisibility for exactly this reason.
    const agentVisibility = useAiAgentVisibility({agentId: agent.id, visibility: agent.visibility});

    const {data: agentTagsData} = useAiAgentTagsQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: currentWorkspaceId != null}
    );

    // TagList works in numeric ids (it is shared with the REST-backed pages); GraphQL serializes every id as a
    // string, so both directions convert at this boundary rather than loosening TagList's own types.
    const agentTags = useMemo(
        () => (agent.tags ?? []).map((tag) => ({id: Number(tag.id), name: tag.name})),
        [agent.tags]
    );

    const remainingTags = useMemo(() => {
        const attachedTagIds = new Set(agentTags.map((tag) => tag.id));

        return (agentTagsData?.aiAgentTags ?? [])
            .map((tag) => ({id: Number(tag.id), name: tag.name}))
            .filter((tag) => !attachedTagIds.has(tag.id));
    }, [agentTagsData?.aiAgentTags, agentTags]);

    // When the agent runs, in words ("Daily at 17:38") rather than as the cron the trigger registers — the
    // reader wants to know when, not to parse five fields. Shown as its own column beside the version badge
    // rather than as a marker's tooltip: a schedule is one of the few things worth knowing about a row at a
    // glance, and hover-to-reveal hides it from anyone scanning the list. Schedule NAMES are left out — they
    // are almost always the agent's own title, which the row already carries. describeCadence falls back to
    // the expression for a row written without the picker, which is the only truthful reading of such a row.
    const scheduleSummary = useMemo(
        () =>
            (agent.channels ?? [])
                .filter((channel) => channel?.channelType === 'schedule')
                .map((channel) => {
                    const parameters = (channel?.parameters ?? {}) as Record<string, unknown>;

                    return describeCadence(fromCadenceParameters(parameters)) || String(parameters.expression ?? '');
                })
                .filter(Boolean)
                .join(', '),
        [agent.channels]
    );

    const deleteAgentMutation = useDeleteAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to delete the agent.');
        },
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiAgents']}),
    });

    const publishAgentMutation = usePublishAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to publish the agent.');
        },
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            toast.success('Agent published.');
        },
    });

    const updateAgentTagsMutation = useUpdateAiAgentTagsMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to update the tags.');
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiAgents']});
            queryClient.invalidateQueries({queryKey: ['aiAgentTags']});
        },
    });

    // The published version wins whenever there is one, even with unpublished edits pending: what the pill
    // reports is what is deployed and running. Only a never-published agent reads as a draft, and it reads V1
    // rather than V0 because its draft is the version that publishing would mint.
    const isDraft = agent.lastPublishedVersion === 0;
    const displayVersion = isDraft ? 1 : agent.lastPublishedVersion;

    const deployable = agent.lastPublishedVersion > 0;

    // The generated enum's values ARE these strings, so this is a representation cast rather than a claim about
    // the value. WORKSPACE is the column default and what a CE server always reports.
    const visibility = (agent.visibility ?? 'WORKSPACE') as ResourceVisibilityValueType;

    const handleDeleteClick = () => {
        deleteAgentMutation.mutate({id: agent.id});
    };

    const handleExportClick = async () => {
        try {
            await exportAgent(agent.id, agent.title);
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to export the agent.');
        }
    };

    // The row opens the agent unless the click reached a control that means something else. Guarding the
    // controls instead — stopPropagation on the column that holds them — also swallowed every click on the
    // empty space around them, so most of the row's right-hand side did nothing.
    const handleClick = (event: React.MouseEvent) => {
        if (isInteractiveElementClick(event.target)) {
            return;
        }

        navigate(`/automation/agents/${agent.id}`);
    };

    return (
        <div
            className="flex cursor-pointer items-center justify-between rounded-md border border-border/50 bg-background p-3"
            onClick={handleClick}
        >
            <div className="flex min-w-0 flex-1 flex-col gap-2">
                <span className="flex min-h-8 items-center gap-1.5 font-semibold">
                    {agent.title}

                    {agentVisibility.enabled && (
                        <span onClick={(event) => event.stopPropagation()}>
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <button
                                        aria-label="Change visibility"
                                        className="cursor-pointer rounded-sm hover:bg-surface-neutral-primary-hover"
                                        type="button"
                                    >
                                        <ResourceVisibilityBadge
                                            grantedUserCount={agentVisibility.grantedUserIds.length}
                                            visibility={visibility}
                                        />
                                    </button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="start" className="p-0">
                                    {/* The caveat rides along with the picker here as well as in the detail
                                        dialog: this dropdown can set an agent PRIVATE, so it has to say what
                                        that does and does not do. */}

                                    <div className="flex max-w-80 min-w-64 flex-col gap-3 p-3">
                                        <ResourceVisibilityPicker
                                            grantedUserIds={agentVisibility.grantedUserIds}
                                            onGrantedUserIdsChange={agentVisibility.onGrantedUserIdsChange}
                                            onVisibilityChange={agentVisibility.onVisibilityChange}
                                            visibility={visibility}
                                            workspaceMembers={agentVisibility.workspaceMembers}
                                        />

                                        <AgentVisibilityCaveat />
                                    </div>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </span>
                    )}
                </span>

                {agent.description && <span className="text-sm text-muted-foreground">{agent.description}</span>}

                {/* TagList centers itself, so a plain block wrapper would stretch and centre it — the extra
                    flex box keeps it sized to its content and left-aligned under the title. w-fit is load
                    bearing: without it the wrapper stretches across the whole column and its stopPropagation
                    swallows every click in the empty space beside the tags. */}

                <div className="flex min-h-7 w-fit items-center" onClick={(event) => event.stopPropagation()}>
                    <TagList
                        getRequest={(id, tags) => ({
                            input: {
                                id: String(id),
                                tags: tags.map((tag) => ({id: tag.id == null ? null : String(tag.id), name: tag.name})),
                            },
                        })}
                        id={+agent.id}
                        remainingTags={remainingTags}
                        tags={agentTags}
                        updateTagsMutation={updateAgentTagsMutation}
                    />
                </div>
            </div>

            <div className="flex shrink-0 items-center gap-8">
                {/* Its own column, ahead of the version badge: the schedule belongs with the row's other
                    at-a-glance facts rather than beside the title, where it would move the title around by
                    whatever the agent happens to be scheduled for. */}

                {isScheduledAgent(agent) && (
                    <span className="flex items-center gap-1 text-sm whitespace-nowrap text-muted-foreground">
                        <CalendarClockIcon className="size-4 shrink-0" />

                        {scheduleSummary || 'Scheduled'}
                    </span>
                )}

                {/* Both columns keep one rhythm — a 32px first row, an 8px gap, a 28px second row — so the
                    two columns come out the same height and the row's items-center lands the published date
                    level with the tags opposite it. Every other *ListItem carries the same three numbers. */}

                <div className="flex flex-col items-end gap-y-2">
                    <div className="flex min-h-8 items-center gap-2">
                        <Badge
                            className="flex space-x-1 bg-surface-neutral-primary"
                            styleType={isDraft ? 'outline-outline' : 'success-outline'}
                            weight="semibold"
                        >
                            <span>V{displayVersion}</span>

                            <span>{isDraft ? 'DRAFT' : 'PUBLISHED'}</span>
                        </Badge>

                        <Button
                            disabled={!deployable}
                            icon={<RocketIcon />}
                            label="Deploy"
                            onClick={() => setShowDeployDialog(true)}
                            size="sm"
                            variant="outline"
                        />
                    </div>

                    <span className="flex min-h-7 items-center text-xs text-muted-foreground">
                        {agent.publishedDate
                            ? `Published at ${new Date(agent.publishedDate).toLocaleString()}`
                            : 'Not yet published'}
                    </span>
                </div>

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            disabled={publishAgentMutation.isPending}
                            onClick={() => publishAgentMutation.mutate({id: agent.id})}
                        >
                            <SendIcon /> Publish
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={() => navigate(`/automation/agents/${agent.id}`)}>
                            <PencilIcon /> Edit
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={handleExportClick}>
                            <DownloadIcon /> Export
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            disabled={deleteAgentMutation.isPending}
                            onClick={handleDeleteClick}
                            variant="destructive"
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showDeployDialog && (
                <ProjectDeploymentDialog
                    onClose={() => setShowDeployDialog(false)}
                    projectDeployment={
                        {
                            environmentId: currentEnvironmentId,
                            projectId: +agent.projectId,
                            projectVersion: agent.lastPublishedVersion,
                        } as ProjectDeployment
                    }
                    redirectOnSubmit={false}
                />
            )}
        </div>
    );
};

export default AgentListItem;
