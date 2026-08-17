import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import exportAgent from '@/pages/automation/agents/utils/agentImportExport';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import TagList from '@/shared/components/TagList';
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
import {DownloadIcon, EllipsisVerticalIcon, PencilIcon, RocketIcon, SendIcon, Trash2Icon} from 'lucide-react';
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
            <div className="flex min-w-0 flex-1 flex-col gap-1">
                <span className="font-semibold">{agent.title}</span>

                {agent.description && <span className="text-sm text-muted-foreground">{agent.description}</span>}

                {/* TagList centers itself, so a plain block wrapper would stretch and centre it — the extra
                    flex box keeps it sized to its content and left-aligned under the title. */}

                <div className="flex" onClick={(event) => event.stopPropagation()}>
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

            <div className="flex shrink-0 items-center gap-4">
                {/* gap-y-4 matches ProjectListItem's right-hand column, so the published date sits the same
                    distance below the version badge on both lists. */}

                <div className="flex flex-col items-end gap-y-4">
                    <div className="flex items-center gap-2">
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

                    <span className="text-xs text-muted-foreground">
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
