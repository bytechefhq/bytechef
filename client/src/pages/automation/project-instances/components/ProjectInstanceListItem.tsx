import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectInstanceListItemAlertDialog from '@/pages/automation/project-instances/components/ProjectInstanceListItemAlertDialog';
import ProjectInstanceListItemDropdownMenu from '@/pages/automation/project-instances/components/ProjectInstanceListItemDropdownMenu';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {ProjectInstanceModel, TagModel} from '@/shared/middleware/automation/configuration';
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

import TagList from '../../../../components/TagList';
import ProjectInstanceDialog from './ProjectInstanceDialog';

interface ProjectInstanceListItemProps {
    projectInstance: ProjectInstanceModel;
    remainingTags?: TagModel[];
}

const ProjectInstanceListItem = ({projectInstance, remainingTags}: ProjectInstanceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showUpdateProjectVersionDialog, setShowUpdateProjectVersionDialog] = useState(false);

    const setProjectInstanceEnabled = useProjectInstancesEnabledStore(
        ({setProjectInstanceEnabled}) => setProjectInstanceEnabled
    );

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
            <div className="flex w-full rounded-md">
                <div className="flex-1 space-y-4 px-4 py-2">
                    <div className="flex w-full items-center space-x-4">
                        {projectInstance.description ? (
                            <Tooltip>
                                <TooltipTrigger>
                                    <h2 className="text-2xl font-semibold leading-3">{projectInstance.name}</h2>
                                </TooltipTrigger>

                                <TooltipContent>{projectInstance.description}</TooltipContent>
                            </Tooltip>
                        ) : (
                            <h2 className="text-2xl font-semibold leading-3">{projectInstance.name}</h2>
                        )}

                        <Badge variant="secondary">v{projectInstance.projectVersion}</Badge>

                        <span className="text-sm font-bold text-gray-500">{projectInstance?.environment}</span>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <CollapsibleTrigger className="group flex items-center space-x-2">
                            <span>
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
                                        updateTagsRequestModel: {
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

                <div className="flex items-center gap-x-6">
                    <div className="flex flex-col items-end gap-y-4">
                        <Badge variant={projectInstance.enabled ? 'success' : 'secondary'}>
                            {projectInstance.enabled ? 'Enabled' : 'Disabled'}
                        </Badge>

                        {projectInstance.lastExecutionDate && (
                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    <span>
                                        {`Executed at ${projectInstance.lastExecutionDate?.toLocaleDateString()} ${projectInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                    </span>
                                </TooltipTrigger>

                                <TooltipContent>Last Execution Date</TooltipContent>
                            </Tooltip>
                        )}
                    </div>

                    <Switch checked={projectInstance.enabled} onCheckedChange={handleOnCheckedChange} />

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

            {showDeleteDialog && (
                <ProjectInstanceListItemAlertDialog
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
