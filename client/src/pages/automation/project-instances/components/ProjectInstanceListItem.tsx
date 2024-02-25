import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectInstanceModel, ProjectModel, TagModel} from '@/middleware/automation/configuration';
import {useUpdateProjectInstanceTagsMutation} from '@/mutations/automation/projectInstanceTags.mutations';
import {
    useDeleteProjectInstanceMutation,
    useEnableProjectInstanceMutation,
} from '@/mutations/automation/projectInstances.mutations';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {ProjectInstanceTagKeys} from '@/queries/automation/projectInstanceTags.queries';
import {ProjectInstanceKeys} from '@/queries/automation/projectInstances.queries';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import TagList from '../../../../components/TagList';
import ProjectInstanceDialog from './ProjectInstanceDialog';

interface ProjectInstanceListItemProps {
    projectInstance: ProjectInstanceModel;
    remainingTags?: TagModel[];
    project: ProjectModel;
}

const ProjectInstanceListItem = ({project, projectInstance, remainingTags}: ProjectInstanceListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
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
            <div className="flex w-full items-center justify-between rounded-md px-2 py-5 hover:bg-gray-50">
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

                            <span className="text-xs uppercase text-gray-700">{project?.name}</span>
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-gray-700">
                                <span className="mr-1">
                                    {project.workflowIds?.length === 1
                                        ? `1 workflow`
                                        : `${project.workflowIds?.length} workflows`}
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
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    <div className="flex flex-col items-end gap-y-4">
                        <Badge variant={projectInstance.enabled ? 'success' : 'secondary'}>
                            {projectInstance.enabled ? 'Enabled' : 'Disabled'}
                        </Badge>

                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {projectInstance.lastExecutionDate ? (
                                    <span>
                                        {`Executed at ${projectInstance.lastExecutionDate?.toLocaleDateString()} ${projectInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Last Execution Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <Switch checked={projectInstance.enabled} onCheckedChange={handleOnCheckedChange} />

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the project and workflows it
                            contains.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-red-600"
                            onClick={() => {
                                if (projectInstance.id) {
                                    deleteProjectInstanceMutation.mutate(projectInstance.id);
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && (
                <ProjectInstanceDialog onClose={() => setShowEditDialog(false)} projectInstance={projectInstance} />
            )}
        </>
    );
};

export default ProjectInstanceListItem;
