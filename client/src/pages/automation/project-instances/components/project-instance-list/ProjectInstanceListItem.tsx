import LoadingIcon from '@/components/LoadingIcon';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectInstanceListItemAlertDialog from '@/pages/automation/project-instances/components/project-instance-list/ProjectInstanceListItemAlertDialog';
import ProjectInstanceListItemDropdownMenu from '@/pages/automation/project-instances/components/project-instance-list/ProjectInstanceListItemDropdownMenu';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {ProjectInstance, Tag} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectInstanceTagsMutation} from '@/shared/mutations/automation/projectInstanceTags.mutations';
import {
    useDeleteProjectInstanceMutation,
    useEnableProjectInstanceMutation,
} from '@/shared/mutations/automation/projectInstances.mutations';
import {ProjectInstanceTagKeys} from '@/shared/queries/automation/projectInstanceTags.queries';
import {ProjectInstanceKeys} from '@/shared/queries/automation/projectInstances.queries';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import TagList from '../../../../../components/TagList';
import ProjectInstanceDialog from '../project-instance-dialog/ProjectInstanceDialog';

interface ProjectInstanceListItemProps {
    projectInstance: ProjectInstance;
    remainingTags?: Tag[];
}

const ProjectInstanceListItem = ({projectInstance, remainingTags}: ProjectInstanceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showUpdateProjectVersionDialog, setShowUpdateProjectVersionDialog] = useState(false);

    const setProjectInstanceEnabled = useProjectInstancesEnabledStore(
        ({setProjectInstanceEnabled}) => setProjectInstanceEnabled
    );

    const {captureProjectInstanceEnabled} = useAnalytics();

    const queryClient = useQueryClient();

    const deleteProjectInstanceMutation = useDeleteProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectInstanceKeys.projectInstances,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectInstanceTagKeys.projectInstanceTags,
            });
        },
    });

    const updateProjectInstanceTagsMutation = useUpdateProjectInstanceTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectInstanceKeys.projectInstances,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectInstanceTagKeys.projectInstanceTags,
            });
        },
    });

    const enableProjectInstanceMutation = useEnableProjectInstanceMutation({
        onSuccess: () => {
            captureProjectInstanceEnabled();

            queryClient.invalidateQueries({
                queryKey: ProjectInstanceKeys.projectInstances,
            });
        },
    });

    const handleOnCheckedChange = (value: boolean) => {
        enableProjectInstanceMutation.mutate(
            {
                enable: value,
                id: projectInstance.id!,
            },
            {
                onSuccess: () => {
                    setProjectInstanceEnabled(projectInstance.id!, !projectInstance.enabled);
                    projectInstance!.enabled = !projectInstance.enabled;
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
                                {projectInstance.description ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="text-base font-semibold">{projectInstance.name}</span>
                                        </TooltipTrigger>

                                        <TooltipContent>{projectInstance.description}</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <span className="text-base font-semibold">{projectInstance.name}</span>
                                )}
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-muted-foreground">
                                    <span className="mr-1">
                                        {projectInstance.projectInstanceWorkflows?.length === 1
                                            ? `1 workflow`
                                            : `${projectInstance.projectInstanceWorkflows?.length} workflows`}
                                    </span>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
                                    {projectInstance.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={projectInstance.id!}
                                            remainingTags={remainingTags}
                                            tags={projectInstance.tags}
                                            updateTagsMutation={updateProjectInstanceTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <Badge variant="secondary">V{projectInstance.projectVersion}</Badge>

                        <div className="flex min-w-28 justify-end">
                            <Badge variant="secondary">{projectInstance.environment}</Badge>
                        </div>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <div className="flex items-center">
                                {enableProjectInstanceMutation.isPending && <LoadingIcon />}

                                <Switch
                                    checked={projectInstance.enabled}
                                    disabled={enableProjectInstanceMutation.isPending}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {projectInstance.lastExecutionDate ? (
                                        <span className="text-xs">
                                            {`Executed at ${projectInstance.lastExecutionDate?.toLocaleDateString()} ${projectInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <ProjectInstanceListItemDropdownMenu
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                            onEnableClick={() =>
                                enableProjectInstanceMutation.mutate({
                                    enable: !projectInstance.enabled,
                                    id: projectInstance.id!,
                                })
                            }
                            onUpdateProjectVersionClick={() => setShowUpdateProjectVersionDialog(true)}
                            projectInstanceEnabled={projectInstance.enabled!}
                        />
                    </div>
                </div>
            </div>

            {showDeleteDialog && (
                <ProjectInstanceListItemAlertDialog
                    isPending={deleteProjectInstanceMutation.isPending}
                    onCancelClick={() => setShowDeleteDialog(false)}
                    onDeleteClick={() => {
                        if (projectInstance.id) {
                            deleteProjectInstanceMutation.mutate(projectInstance.id);
                        }
                    }}
                />
            )}

            {showEditDialog && (
                <ProjectInstanceDialog onClose={() => setShowEditDialog(false)} projectInstance={projectInstance} />
            )}

            {showUpdateProjectVersionDialog && (
                <ProjectInstanceDialog
                    onClose={() => setShowUpdateProjectVersionDialog(false)}
                    projectInstance={projectInstance}
                    updateProjectVersion={true}
                />
            )}
        </>
    );
};

export default ProjectInstanceListItem;
