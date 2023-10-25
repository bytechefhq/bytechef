import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ProjectInstanceModel,
    ProjectModel,
    TagModel,
} from '@/middleware/helios/configuration';
import {
    useDeleteProjectInstanceMutation,
    useEnableProjectInstanceMutation,
    useUpdateProjectInstanceTagsMutation,
} from '@/mutations/projects.mutations';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {ProjectKeys} from '@/queries/projects.queries';
import {AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';

import AlertDialog from '../../../components/AlertDialog/AlertDialog';
import Badge from '../../../components/Badge/Badge';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import HoverCard from '../../../components/HoverCard/HoverCard';
import TagList from '../../../components/TagList/TagList';
import ProjectInstanceDialog from './ProjectInstanceDialog';

interface ProjectItemProps {
    projectInstance: ProjectInstanceModel;
    remainingTags?: TagModel[];
    project: ProjectModel;
}

const ProjectInstanceListItem = ({
    project,
    projectInstance,
    remainingTags,
}: ProjectItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const setProjectInstanceEnabled = useProjectInstancesEnabledStore(
        ({setProjectInstanceEnabled}) => setProjectInstanceEnabled
    );

    const queryClient = useQueryClient();

    const deleteProjectInstanceMutation = useDeleteProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectInstances);
            queryClient.invalidateQueries(ProjectKeys.projectInstanceTags);
        },
    });

    const updateProjectInstanceTagsMutation =
        useUpdateProjectInstanceTagsMutation({
            onSuccess: () => {
                queryClient.invalidateQueries(ProjectKeys.projectInstances);
                queryClient.invalidateQueries(ProjectKeys.projectInstanceTags);
            },
        });

    const dropdownItems: IDropdownMenuItem[] = [
        {
            label: 'Edit',
            onClick: () => setShowEditDialog(true),
        },
        {
            separator: true,
        },
        {
            danger: true,
            label: 'Delete',
            onClick: () => setShowDeleteDialog(true),
        },
    ];

    const enableProjectInstanceMutation = useEnableProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectInstances);
        },
    });

    return (
        <>
            <div className="flex items-center justify-between">
                <AccordionTrigger className="group w-10/12">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center justify-between">
                            {projectInstance.description ? (
                                <HoverCard text={projectInstance.description}>
                                    <span className="mr-2 text-base font-semibold text-gray-900">
                                        {projectInstance.name}
                                    </span>
                                </HoverCard>
                            ) : (
                                <span className="mr-2 text-base font-semibold text-gray-900">
                                    {projectInstance.name}
                                </span>
                            )}
                        </div>

                        <div className="ml-2 flex shrink-0">
                            <Badge
                                color={
                                    projectInstance.enabled
                                        ? 'green'
                                        : 'default'
                                }
                                text={
                                    projectInstance.enabled
                                        ? 'Enabled'
                                        : 'Disabled'
                                }
                            />
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            <div className="mr-4 flex text-xs font-semibold text-gray-700">
                                <span className="mr-1">
                                    {project.workflowIds?.length === 1
                                        ? `1 workflow`
                                        : `${project.workflowIds?.length} workflows`}
                                </span>

                                <ChevronDownIcon className="h-4 w-4 duration-300 group-data-[state=open]:rotate-180" />
                            </div>

                            <div onClick={(event) => event.preventDefault()}>
                                {projectInstance.tags && (
                                    <TagList
                                        id={projectInstance.id!}
                                        remainingTags={remainingTags}
                                        tags={projectInstance.tags}
                                        updateTagsMutation={
                                            updateProjectInstanceTagsMutation
                                        }
                                        getRequest={(id, tags) => ({
                                            id: id!,
                                            updateTagsRequestModel: {
                                                tags: tags || [],
                                            },
                                        })}
                                    />
                                )}
                            </div>
                        </div>

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            {projectInstance.lastExecutionDate ? (
                                <>
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <div className="flex text-sm text-gray-500">
                                                <CalendarIcon
                                                    className="mr-1 h-5 w-5 shrink-0 text-gray-400"
                                                    aria-hidden="true"
                                                />

                                                <span>
                                                    {`${projectInstance.lastExecutionDate?.toLocaleDateString()} ${projectInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                                </span>
                                            </div>
                                        </TooltipTrigger>

                                        <TooltipContent>
                                            Last Execution Date
                                        </TooltipContent>
                                    </Tooltip>
                                </>
                            ) : (
                                '-'
                            )}
                        </div>
                    </div>
                </AccordionTrigger>

                <div className="flex w-1/12 items-center justify-end">
                    <Switch
                        checked={projectInstance.enabled}
                        onCheckedChange={(value) => {
                            enableProjectInstanceMutation.mutate(
                                {
                                    enable: value,
                                    id: projectInstance.id!,
                                },
                                {
                                    onSuccess: () => {
                                        setProjectInstanceEnabled(
                                            projectInstance.id!,
                                            !projectInstance.enabled
                                        );
                                        projectInstance!.enabled =
                                            !projectInstance.enabled;
                                    },
                                }
                            );
                        }}
                    />
                </div>

                <div className="flex w-1/12 justify-end">
                    <DropdownMenu
                        id={projectInstance.id}
                        menuItems={dropdownItems}
                    />
                </div>
            </div>

            {showDeleteDialog && (
                <AlertDialog
                    danger
                    isOpen
                    message="This action cannot be undone. This will permanently delete the project and workflows it contains."
                    title="Are you absolutely sure?"
                    setIsOpen={setShowDeleteDialog}
                    onConfirmClick={() => {
                        if (projectInstance.id) {
                            deleteProjectInstanceMutation.mutate(
                                projectInstance.id
                            );
                        }
                    }}
                />
            )}

            {showEditDialog && (
                <ProjectInstanceDialog
                    projectInstance={projectInstance}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditDialog(false)}
                />
            )}
        </>
    );
};

export default ProjectInstanceListItem;
