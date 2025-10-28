import LoadingIcon from '@/components/LoadingIcon';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentListItemAlertDialog from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentListItemAlertDialog';
import ProjectDeploymentListItemDropdownMenu from '@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentListItemDropdownMenu';
import {useProjectDeploymentsEnabledStore} from '@/pages/automation/project-deployments/stores/useProjectDeploymentsEnabledStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {ProjectDeployment, Tag} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectDeploymentTagsMutation} from '@/shared/mutations/automation/projectDeploymentTags.mutations';
import {
    useDeleteProjectDeploymentMutation,
    useEnableProjectDeploymentMutation,
} from '@/shared/mutations/automation/projectDeployments.mutations';
import {ProjectDeploymentTagKeys} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon} from 'lucide-react';
import {useState} from 'react';

import TagList from '../../../../../shared/components/TagList';
import ProjectDeploymentDialog from '../project-deployment-dialog/ProjectDeploymentDialog';

interface ProjectDeploymentListItemProps {
    projectDeployment: ProjectDeployment;
    remainingTags?: Tag[];
}

const ProjectDeploymentListItem = ({projectDeployment, remainingTags}: ProjectDeploymentListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showUpdateProjectVersionDialog, setShowUpdateProjectVersionDialog] = useState(false);

    const setProjectDeploymentEnabled = useProjectDeploymentsEnabledStore(
        ({setProjectDeploymentEnabled}) => setProjectDeploymentEnabled
    );

    const {captureProjectDeploymentEnabled} = useAnalytics();

    const queryClient = useQueryClient();

    const deleteProjectDeploymentMutation = useDeleteProjectDeploymentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentKeys.projectDeployments,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentTagKeys.projectDeploymentTags,
            });
        },
    });

    const updateProjectDeploymentTagsMutation = useUpdateProjectDeploymentTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentKeys.projectDeployments,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentTagKeys.projectDeploymentTags,
            });
        },
    });

    const enableProjectDeploymentMutation = useEnableProjectDeploymentMutation({
        onSuccess: () => {
            captureProjectDeploymentEnabled();

            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentKeys.projectDeployments,
            });
        },
    });

    const handleOnCheckedChange = (value: boolean) => {
        enableProjectDeploymentMutation.mutate(
            {
                enable: value,
                id: projectDeployment.id!,
            },
            {
                onSuccess: () => {
                    setProjectDeploymentEnabled(projectDeployment.id!, !projectDeployment.enabled);
                    projectDeployment!.enabled = !projectDeployment.enabled;
                },
            }
        );
    };

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
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
                                <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-muted-foreground">
                                    <span className="mr-1">
                                        {projectDeployment.projectDeploymentWorkflows?.length === 1
                                            ? `1 workflow`
                                            : `${projectDeployment.projectDeploymentWorkflows?.length} workflows`}
                                    </span>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
                                    {projectDeployment.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
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
                                <Badge variant="secondary">V{projectDeployment.projectVersion}</Badge>
                            </TooltipTrigger>

                            <TooltipContent>The project version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <div className="flex items-center">
                                {enableProjectDeploymentMutation.isPending && <LoadingIcon />}

                                <Switch
                                    checked={projectDeployment.enabled}
                                    disabled={enableProjectDeploymentMutation.isPending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
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
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                            onUpdateProjectVersionClick={() => setShowUpdateProjectVersionDialog(true)}
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
                />
            )}

            {showUpdateProjectVersionDialog && (
                <ProjectDeploymentDialog
                    onClose={() => setShowUpdateProjectVersionDialog(false)}
                    projectDeployment={projectDeployment}
                    updateProjectVersion={true}
                />
            )}
        </>
    );
};

export default ProjectDeploymentListItem;
