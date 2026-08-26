import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentListItemAlertDialog from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentListItemAlertDialog';
import ProjectDeploymentListItemDropdownMenu from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentListItemDropdownMenu';
import {useProjectDeploymentsEnabledStore} from '@/pages/automation/project-deployments/stores/useProjectDeploymentsEnabledStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EEVersion from '@/shared/edition/EEVersion';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {ProjectDeployment, Tag} from '@/shared/middleware/automation/configuration';
import {PromotionResourceType, useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useUpdateProjectDeploymentTagsMutation} from '@/shared/mutations/automation/projectDeploymentTags.mutations';
import {
    useDeleteProjectDeploymentMutation,
    useEnableProjectDeploymentMutation,
} from '@/shared/mutations/automation/projectDeployments.mutations';
import {ProjectDeploymentTagKeys} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import isInteractiveElementClick from '@/shared/util/interactive-element-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon} from 'lucide-react';
import {Suspense, lazy, useCallback, useRef, useState} from 'react';

import TagList from '../../../../../shared/components/TagList';
import ProjectDeploymentDialog from '../project-deployment-dialog/ProjectDeploymentDialog';

const EnvironmentPromotionDialog = lazy(
    () => import('@/ee/shared/components/environment-promotion/EnvironmentPromotionDialog')
);

interface ProjectDeploymentListItemProps {
    projectDeployment: ProjectDeployment;
    remainingTags?: Tag[];
}

const ProjectDeploymentListItem = ({projectDeployment, remainingTags}: ProjectDeploymentListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showChangeProjectVersionDialog, setShowChangeProjectVersionDialog] = useState(false);
    const [showPromotionDialog, setShowPromotionDialog] = useState(false);

    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const setProjectDeploymentEnabled = useProjectDeploymentsEnabledStore(
        ({setProjectDeploymentEnabled}) => setProjectDeploymentEnabled
    );

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {captureProjectDeploymentEnabled} = useAnalytics();

    const queryClient = useQueryClient();

    const environmentsQuery = useEnvironmentsQuery();

    const deleteProjectDeploymentMutation = useDeleteProjectDeploymentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentTagKeys.projectDeploymentTags(currentWorkspaceId!),
            });
        },
    });

    const updateProjectDeploymentTagsMutation = useUpdateProjectDeploymentTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentTagKeys.projectDeploymentTags(currentWorkspaceId!),
            });
        },
    });

    const enableProjectDeploymentMutation = useEnableProjectDeploymentMutation({
        onSuccess: () => {
            captureProjectDeploymentEnabled();
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
        },
    });

    const handleOnCheckedChange = (value: boolean) => {
        enableProjectDeploymentMutation.mutate(
            {enable: value, id: projectDeployment.id!},
            {
                onSuccess: () => {
                    setProjectDeploymentEnabled(projectDeployment.id!, !projectDeployment.enabled);
                    projectDeployment!.enabled = !projectDeployment.enabled;
                },
            }
        );
    };

    const handleProjectDeploymentListItemClick = useCallback((event: React.MouseEvent) => {
        if (isInteractiveElementClick(event.target)) {
            return;
        }

        if (workflowsCollapsibleTriggerRef.current?.contains(event.target as Node)) {
            return;
        }

        workflowsCollapsibleTriggerRef.current?.click();
    }, []);

    const enabledProjectDeploymentWorkflows =
        projectDeployment.projectDeploymentWorkflows?.filter((workflow) => workflow.enabled) ?? [];

    const enabledWorkflowCount = enabledProjectDeploymentWorkflows.length;

    const isDeploymentSwitchDisabled =
        enableProjectDeploymentMutation.isPending || (enabledWorkflowCount < 1 && !projectDeployment.enabled);

    const showPromoteToEnvironment = (environmentsQuery.data?.environments?.length ?? 0) >= 2;

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-destructive-foreground"
                onClick={(event) => handleProjectDeploymentListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center gap-2">
                                {projectDeployment.description ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="text-base font-semibold">{projectDeployment.name}</span>
                                        </TooltipTrigger>

                                        <TooltipContent>{projectDeployment.description}</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <span className="text-base font-semibold">{projectDeployment.name}</span>
                                )}
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger
                                    className="group mr-4 flex text-xs font-semibold text-muted-foreground"
                                    ref={workflowsCollapsibleTriggerRef}
                                >
                                    <span className="mr-1">
                                        {projectDeployment.projectDeploymentWorkflows?.length === 1
                                            ? `1 workflow`
                                            : `${projectDeployment.projectDeploymentWorkflows?.length} workflows`}
                                    </span>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.stopPropagation()}>
                                    {projectDeployment.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {tags: tags || []},
                                            })}
                                            id={projectDeployment.id!}
                                            remainingTags={remainingTags}
                                            tags={projectDeployment.tags}
                                            updateTagsMutation={updateProjectDeploymentTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`V${projectDeployment.projectVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>The project version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <div className="flex items-center">
                                {enableProjectDeploymentMutation.isPending && <LoadingIcon />}

                                <Switch
                                    checked={projectDeployment.enabled}
                                    disabled={isDeploymentSwitchDisabled}
                                    onCheckedChange={handleOnCheckedChange}
                                    onClick={(event) => event.stopPropagation()}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                                    {projectDeployment.lastExecutionDate ? (
                                        <span className="text-xs">
                                            {`Executed at ${projectDeployment.lastExecutionDate?.toLocaleDateString()} ${projectDeployment.lastExecutionDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <ProjectDeploymentListItemDropdownMenu
                            onChangeProjectVersionClick={() => setShowChangeProjectVersionDialog(true)}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                            onPromoteClick={() => setShowPromotionDialog(true)}
                            showPromoteToEnvironment={showPromoteToEnvironment}
                        />
                    </div>
                </div>
            </div>

            {showDeleteDialog && (
                <ProjectDeploymentListItemAlertDialog
                    isPending={deleteProjectDeploymentMutation.isPending}
                    onCancelClick={() => setShowDeleteDialog(false)}
                    onDeleteClick={() => {
                        if (projectDeployment.id) {
                            deleteProjectDeploymentMutation.mutate(projectDeployment.id);
                        }
                    }}
                />
            )}

            {showEditDialog && (
                <ProjectDeploymentDialog
                    onClose={() => setShowEditDialog(false)}
                    projectDeployment={projectDeployment}
                    redirectOnSubmit={false}
                />
            )}

            {showChangeProjectVersionDialog && (
                <ProjectDeploymentDialog
                    changeProjectVersion={true}
                    onClose={() => setShowChangeProjectVersionDialog(false)}
                    projectDeployment={projectDeployment}
                    redirectOnSubmit={false}
                />
            )}

            {showPromotionDialog && projectDeployment.id != null && projectDeployment.environmentId != null && (
                <EEVersion hidden={true}>
                    <Suspense fallback={null}>
                        <EnvironmentPromotionDialog
                            onClose={() => setShowPromotionDialog(false)}
                            onPromoted={() => {
                                queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
                            }}
                            resourceType={PromotionResourceType.ProjectDeployment}
                            sourceEnvironmentId={projectDeployment.environmentId}
                            sourceId={String(projectDeployment.id)}
                            sourceName={projectDeployment.name}
                            workspaceId={currentWorkspaceId!}
                        />
                    </Suspense>
                </EEVersion>
            )}
        </>
    );
};

export default ProjectDeploymentListItem;
