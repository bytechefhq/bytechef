import {CalendarIcon} from '@heroicons/react/24/outline';
import {useQueryClient} from '@tanstack/react-query';
import React, {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';

import AlertDialog from '../../../components/AlertDialog/AlertDialog';
import Badge from '../../../components/Badge/Badge';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import HoverCard from '../../../components/HoverCard/HoverCard';
import TagList from '../../../components/TagList/TagList';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';
import {
    ProjectModel,
    ProjectModelStatusEnum,
    TagModel,
} from '../../../middleware/automation/project';
import {
    useCreateProjectWorkflowRequestMutation,
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
    useUpdateProjectTagsMutation,
} from '../../../mutations/projects.mutations';
import {
    ProjectKeys,
    useGetProjectWorkflowsQuery,
} from '../../../queries/projects.queries';
import ProjectDialog from './ProjectDialog';

interface ProjectItemProps {
    project: ProjectModel;
    remainingTags?: TagModel[];
}

const ProjectListItem = ({project, remainingTags}: ProjectItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(
        project?.id as number
    );

    const createProjectWorkflowRequestMutation =
        useCreateProjectWorkflowRequestMutation({
            onSuccess: (workflow) => {
                if (projectWorkflows) {
                    navigate(
                        `/automation/projects/${project.id}/workflow/${workflow?.id}`
                    );
                }

                setShowWorkflowDialog(false);
            },
        });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projects);
            queryClient.invalidateQueries(ProjectKeys.projectCategories);
            queryClient.invalidateQueries(ProjectKeys.projectTags);
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projects);
        },
    });

    const updateProjectTagsMutation = useUpdateProjectTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projects);
            queryClient.invalidateQueries(ProjectKeys.projectTags);
        },
    });

    const initialWorkflowId = project.workflowIds![0];

    const dropdownItems: IDropdownMenuItem[] = [
        {
            label: 'Edit',
            onClick: () => setShowEditDialog(true),
        },
        {
            label: 'Duplicate',
            onClick: () => duplicateProjectMutation.mutate(project.id!),
        },
        {
            label: 'New Workflow',
            onClick: () => setShowWorkflowDialog(true),
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
                {initialWorkflowId && (
                    <Link
                        className="flex-1 pr-8"
                        to={`/automation/projects/${project.id}/workflow/${initialWorkflowId}`}
                    >
                        <div className="flex items-center justify-between">
                            <div className="relative flex items-center">
                                {project.description ? (
                                    <HoverCard text={project.description}>
                                        <span className="mr-2 text-base font-semibold text-gray-900">
                                            {project.name}
                                        </span>
                                    </HoverCard>
                                ) : (
                                    <span className="mr-2 text-base font-semibold text-gray-900">
                                        {project.name}
                                    </span>
                                )}

                                {project.category && (
                                    <span className="text-xs uppercase text-gray-700">
                                        {project.category.name}
                                    </span>
                                )}
                            </div>

                            <div className="ml-2 flex shrink-0">
                                <Badge
                                    color={
                                        project.status ===
                                        ProjectModelStatusEnum.Published
                                            ? 'green'
                                            : 'default'
                                    }
                                    text={
                                        project.status ===
                                        ProjectModelStatusEnum.Published
                                            ? `Published V${project.projectVersion}`
                                            : 'Not Published'
                                    }
                                />
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div
                                className="flex h-[38px] items-center"
                                onClick={(event) => event.preventDefault()}
                            >
                                <div className="mr-4 text-xs font-semibold text-gray-700">
                                    {project.workflowIds?.length === 1
                                        ? `${project.workflowIds?.length} workflow`
                                        : `${project.workflowIds?.length} workflows`}
                                </div>

                                {project.tags && (
                                    <TagList
                                        id={project.id!}
                                        remainingTags={remainingTags}
                                        tags={project.tags}
                                        updateTagsMutation={
                                            updateProjectTagsMutation
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
                                {project.status ===
                                ProjectModelStatusEnum.Published ? (
                                    <>
                                        <CalendarIcon
                                            className="mr-1 h-5 w-5 shrink-0 text-gray-400"
                                            aria-hidden="true"
                                        />

                                        <span>
                                            {project.publishedDate?.toLocaleDateString()}
                                        </span>
                                    </>
                                ) : (
                                    '-'
                                )}
                            </div>
                        </div>
                    </Link>
                )}

                <DropdownMenu id={project.id} menuItems={dropdownItems} />
            </div>

            {showDeleteDialog && (
                <AlertDialog
                    danger
                    isOpen
                    message="This action cannot be undone. This will permanently delete the project and workflows it contains."
                    title="Are you absolutely sure?"
                    setIsOpen={setShowDeleteDialog}
                    onConfirmClick={() => {
                        if (project.id) {
                            deleteProjectMutation.mutate(project.id);
                        }
                    }}
                />
            )}

            {showEditDialog && (
                <ProjectDialog
                    project={project}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditDialog(false)}
                />
            )}

            {showWorkflowDialog && !!project.id && (
                <WorkflowDialog
                    id={project.id}
                    showTrigger={false}
                    visible
                    createWorkflowRequestMutation={
                        createProjectWorkflowRequestMutation
                    }
                />
            )}
        </>
    );
};

export default ProjectListItem;
