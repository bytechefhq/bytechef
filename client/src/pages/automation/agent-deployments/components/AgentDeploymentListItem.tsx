import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AgentDeploymentChannelList, {
    WORKFLOW_CALL_TRIGGER_TYPE,
} from '@/pages/automation/agent-deployments/components/AgentDeploymentChannelList';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import ProjectDeploymentListItemAlertDialog from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentListItemAlertDialog';
import ProjectDeploymentListItemDropdownMenu from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentListItemDropdownMenu';
import TagList from '@/shared/components/TagList';
import {AiAgentDeployment, useUpdateAiAgentDeploymentTagsMutation} from '@/shared/middleware/graphql';
import {Tag} from '@/shared/middleware/platform/configuration';
import {
    useDeleteProjectDeploymentMutation,
    useEnableProjectDeploymentMutation,
} from '@/shared/mutations/automation/projectDeployments.mutations';
import {
    ProjectDeploymentKeys,
    useGetProjectDeploymentQuery,
} from '@/shared/queries/automation/projectDeployments.queries';
import isInteractiveElementClick from '@/shared/util/interactive-element-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon} from 'lucide-react';
import {useCallback, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';

interface AgentDeploymentListItemProps {
    deployment: AiAgentDeployment;
    /** Every tag used by an agent deployment in the workspace, for the add-tag picker. */
    remainingTags?: Tag[];
}

const AgentDeploymentListItem = ({deployment, remainingTags}: AgentDeploymentListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const channelsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    // Fetched (rather than assembled from `deployment`) exactly like ApiCollectionListItem.tsx:45 does for its own
    // change-project-version dialog: ProjectDeploymentFacadeImpl.getProjectDeployment(long) is a plain unfiltered
    // findById (unlike every projectId/workspace-scoped listing path), so it returns the full REST shape — name,
    // tags, and each workflow's existing enabled/connection state — that ProjectDeploymentDialog's "change version"
    // step needs to pre-populate correctly, none of which the slimmer agentDeployments GraphQL query carries.
    const {data: projectDeployment} = useGetProjectDeploymentQuery(+deployment.id, showEditDialog);

    const queryClient = useQueryClient();

    // Same rule as ProjectDeploymentListItem.tsx:66-83: pending or (zero enabled workflows and currently disabled).
    const enableAgentDeploymentMutation = useEnableProjectDeploymentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
        },
    });

    const deleteAgentDeploymentMutation = useDeleteProjectDeploymentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});

            // This list is the aiAgentDeployments GraphQL query, which the REST keys above do not reach.
            queryClient.invalidateQueries({queryKey: ['aiAgentDeployments']});
        },
    });

    const enabledWorkflowCount = deployment.workflows.filter((workflow) => workflow.enabled).length;

    const isSwitchDisabled =
        enableAgentDeploymentMutation.isPending || (enabledWorkflowCount < 1 && !deployment.enabled);

    const handleOnCheckedChange = (value: boolean) => {
        enableAgentDeploymentMutation.mutate(
            {enable: value, id: +deployment.id},
            {
                onSuccess: () => {
                    deployment.enabled = !deployment.enabled;
                },
            }
        );
    };

    // The channels actually listed below, not the workflow count — an agent deploys ONE workflow carrying every
    // channel as a trigger, so counting workflows always said "1 channel". workflowCall is excluded for the same
    // reason the list excludes it.
    const channelCount = deployment.workflows.reduce(
        (count, workflow) =>
            count + workflow.triggers.filter((trigger) => trigger.type !== WORKFLOW_CALL_TRIGGER_TYPE).length,
        0
    );

    // TagList works in numeric ids (it is shared with the REST-backed pages); GraphQL serializes every id as a
    // string, so both directions convert at this boundary rather than loosening TagList's own types.
    const deploymentTags = useMemo(
        () => (deployment.tags ?? []).map((tag) => ({id: Number(tag.id), name: tag.name})),
        [deployment.tags]
    );

    const updateAgentDeploymentTagsMutation = useUpdateAiAgentDeploymentTagsMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to update the tags.');
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiAgentDeployments']});
            queryClient.invalidateQueries({queryKey: ['aiAgentDeploymentTags']});
        },
    });

    // Clicking the row toggles the channel list, the way ProjectDeploymentListItem toggles its workflows —
    // the "N channels" line is a small target for what is the row's main action. Clicks that reach the
    // switch, the menu or the tag editor are theirs; the trigger's own clicks are left alone so it does not
    // toggle twice.
    const handleClick = useCallback((event: React.MouseEvent) => {
        if (isInteractiveElementClick(event.target)) {
            return;
        }

        channelsCollapsibleTriggerRef.current?.click();
    }, []);

    // Serialized as a string over GraphQL (the schema has no date scalar), so it is parsed here rather than
    // trusted to arrive as a Date.
    const lastExecutionDate = deployment.lastExecutionDate ? new Date(deployment.lastExecutionDate) : undefined;

    return (
        <Collapsible className="group mb-2 rounded border border-border/50">
            {/* Mirrors ProjectDeploymentListItem: the title stands alone, and the collapse control is the
                "N channels" line beneath it rather than a chevron in its own left-hand column. The environment
                is not repeated per row — the page header's environment select already scopes the whole list. */}

            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-destructive-foreground"
                onClick={handleClick}
            >
                <div className="flex flex-1 items-center py-3">
                    <div className="flex-1">
                        <span className="flex min-h-8 items-center text-base font-semibold">
                            {deployment.agentTitle}
                        </span>

                        <div className="mt-2 flex min-h-7 items-center">
                            <CollapsibleTrigger
                                className="group mr-4 flex text-xs font-semibold text-muted-foreground"
                                data-interactive
                                ref={channelsCollapsibleTriggerRef}
                            >
                                <span className="mr-1">
                                    {channelCount === 1 ? '1 channel' : `${channelCount} channels`}
                                </span>

                                <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                            </CollapsibleTrigger>

                            {/* The deployment's OWN tags, not the owning agent's: an agent deployment is a
                                ProjectDeployment, so these are its project-deployment tags, edited through the
                                agent-deployment mutation pair that wraps them. */}

                            <div onClick={(event) => event.stopPropagation()}>
                                <TagList
                                    getRequest={(id, updatedTags) => ({
                                        input: {
                                            id: String(id),
                                            tags: updatedTags.map((tag) => ({
                                                id: tag.id == null ? null : String(tag.id),
                                                name: tag.name,
                                            })),
                                        },
                                    })}
                                    id={+deployment.id}
                                    remainingTags={remainingTags?.filter(
                                        (tag) => !deploymentTags.some((deploymentTag) => deploymentTag.id === tag.id)
                                    )}
                                    tags={deploymentTags}
                                    updateTagsMutation={updateAgentDeploymentTagsMutation}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <Badge label={`V${deployment.projectVersion}`} styleType="secondary-filled" weight="semibold" />

                        {/* The min-w column is what puts the toggle above its caption and keeps both right-aligned,
                            matching ProjectDeploymentListItem. */}

                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="flex min-h-8 items-center">
                                {enableAgentDeploymentMutation.isPending && <LoadingIcon />}

                                <Switch
                                    checked={deployment.enabled}
                                    disabled={isSwitchDisabled}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
                                    <span className="text-xs">
                                        {lastExecutionDate
                                            ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                            : 'No executions'}
                                    </span>
                                </TooltipTrigger>

                                <TooltipContent>Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        {/* The same menu the project deployment rows use, so both lists offer the same actions
                            in the same place — a lone pencil offered no way to delete a deployment at all. */}

                        <ProjectDeploymentListItemDropdownMenu
                            changeVersionLabel="Change Agent Version"
                            onChangeProjectVersionClick={() => setShowEditDialog(true)}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                        />
                    </div>
                </div>
            </div>

            <CollapsibleContent>
                {/* pt-3 above the heading and none below the rows, mirroring ProjectDeploymentWorkflowList —
                    the heading needs separating from the row above it, the rows do not from the card edge. */}

                <div className="pt-3">
                    <h3 className="flex justify-start pl-3 text-sm heading-tertiary">Channels</h3>

                    <AgentDeploymentChannelList
                        projectDeploymentId={deployment.id}
                        title={deployment.agentTitle}
                        workflows={deployment.workflows}
                    />
                </div>
            </CollapsibleContent>

            {showDeleteDialog && (
                <ProjectDeploymentListItemAlertDialog
                    onCancelClick={() => setShowDeleteDialog(false)}
                    onDeleteClick={() => {
                        deleteAgentDeploymentMutation.mutate(+deployment.id);

                        setShowDeleteDialog(false);
                    }}
                />
            )}

            {showEditDialog && projectDeployment && (
                <ProjectDeploymentDialog
                    changeProjectVersion
                    onClose={() => setShowEditDialog(false)}
                    projectDeployment={projectDeployment}
                    redirectOnSubmit={false}
                />
            )}
        </Collapsible>
    );
};

export default AgentDeploymentListItem;
