import {CalendarIcon, ChevronDownIcon} from '@heroicons/react/24/outline';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

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
} from '../../../middleware/automation/configuration';
import {
    useCreateProjectWorkflowRequestMutation,
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
    useUpdateProjectTagsMutation,
} from '../../../mutations/projects.mutations';
import {ProjectKeys} from '../../../queries/projects.queries';
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

    const createProjectWorkflowRequestMutation =
        useCreateProjectWorkflowRequestMutation({
            onSuccess: (workflow) => {
                navigate(
                    `/automation/projects/${project.id}/workflow/${workflow?.id}`
                );

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
            <li className="flex items-center rounded border bg-white p-4 hover:bg-gray-50 group-radix-state-open:rounded-b-none group-radix-state-open:border-b-0">
                <div className="flex-1 pr-8">
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

                    <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                        <div
                            className="flex items-center"
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

                        <ChevronDownIcon className="absolute left-1/2 top-1/2 h-5 w-5 -translate-x-1/2 -translate-y-1/2 text-gray-400 transition-transform duration-300 ease-[cubic-bezier(0.87,_0,_0.13,_1)] group-data-[state=open]:rotate-180" />

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
                </div>

                <DropdownMenu id={project.id} menuItems={dropdownItems} />
            </li>

            <AlertDialog
                danger
                isOpen={showDeleteDialog}
                message="This action cannot be undone. This will permanently delete the project and workflows it contains."
                title="Are you absolutely sure?"
                setIsOpen={setShowDeleteDialog}
                onConfirmClick={() => {
                    if (project.id) {
                        deleteProjectMutation.mutate(project.id);
                    }
                }}
            />

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
