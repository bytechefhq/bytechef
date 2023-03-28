import {CalendarIcon} from '@heroicons/react/24/outline';
import {useQueryClient} from '@tanstack/react-query';
import React, {useState} from 'react';

import AlertDialog from '../../../components/AlertDialog/AlertDialog';
import Badge from '../../../components/Badge/Badge';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import HoverCard from '../../../components/HoverCard/HoverCard';
import TagList from '../../../components/TagList/TagList';
import {
    ProjectInstanceModel,
    ProjectInstanceModelStatusEnum,
    TagModel,
} from '../../../middleware/project';
import {
    useDeleteProjectInstanceMutation,
    useUpdateProjectInstanceTagsMutation,
} from '../../../mutations/projects.mutations';
import {ProjectKeys} from '../../../queries/projects.queries';
import ProjectInstanceDialog from './ProjectInstanceDialog';

interface ProjectItemProps {
    projectInstance: ProjectInstanceModel;
    remainingTags?: TagModel[];
}

const ProjectInstanceListItem = ({
    projectInstance,
    remainingTags,
}: ProjectItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

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

    return (
        <>
            <div className="flex items-center">
                <div className="flex-1 pr-8">
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center">
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

                            {projectInstance.project && (
                                <span className="text-xs uppercase text-gray-700">
                                    {`${projectInstance.project.name} V${projectInstance.project.projectVersion}`}
                                </span>
                            )}
                        </div>

                        <div className="ml-2 flex shrink-0">
                            <Badge
                                color={
                                    projectInstance.status ===
                                    ProjectInstanceModelStatusEnum.Enabled
                                        ? 'green'
                                        : 'default'
                                }
                                text={
                                    projectInstance.status ===
                                    ProjectInstanceModelStatusEnum.Enabled
                                        ? 'Enabled'
                                        : 'Disabled'
                                }
                            />
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div
                            className="flex h-[38px] items-center"
                            onClick={(event) => event.preventDefault()}
                        >
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

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            {projectInstance.status ===
                            ProjectInstanceModelStatusEnum.Enabled ? (
                                <>
                                    <CalendarIcon
                                        className="mr-1 h-5 w-5 shrink-0 text-gray-400"
                                        aria-hidden="true"
                                    />

                                    <span>
                                        {`${projectInstance.lastExecutionDate?.toLocaleDateString()} ${projectInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                    </span>
                                </>
                            ) : (
                                '-'
                            )}
                        </div>
                    </div>
                </div>

                <DropdownMenu
                    id={projectInstance.id}
                    menuItems={dropdownItems}
                />
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
