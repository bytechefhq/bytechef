import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectModel} from '@/middleware/automation/configuration';
import {WorkflowTestApi} from '@/middleware/platform/workflow/test';
import {useDeleteProjectMutation} from '@/mutations/automation/projects.mutations';
import {useCreateProjectWorkflowMutation, useDeleteWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import ProjectHeaderDeleteProjectAlertDialog from '@/pages/automation/project/components/ProjectHeaderDeleteProjectAlertDialog';
import ProjectHeaderDeleteWorkflowAlertDialog from '@/pages/automation/project/components/ProjectHeaderDeleteWorkflowAlertDialog';
import ProjectHeaderOutputButton from '@/pages/automation/project/components/ProjectHeaderOutputButton';
import ProjectHeaderProjectDropDownMenu from '@/pages/automation/project/components/ProjectHeaderProjectDropDownMenu';
import ProjectHeaderProjectVersionBadge from '@/pages/automation/project/components/ProjectHeaderProjectVersionBadge';
import ProjectHeaderRunButton from '@/pages/automation/project/components/ProjectHeaderRunButton';
import ProjectHeaderStopButton from '@/pages/automation/project/components/ProjectHeaderStopButton';
import ProjectHeaderWorkflowDropDownMenu from '@/pages/automation/project/components/ProjectHeaderWorkflowDropDownMenu';
import ProjectHeaderWorkflowSelect from '@/pages/automation/project/components/ProjectHeaderWorkflowSelect';
import ProjectPublishPopover from '@/pages/automation/project/components/ProjectPublishPopover';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {ProjectCategoryKeys} from '@/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/automation/projectTags.queries';
import {ProjectWorkflowKeys} from '@/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetProjectQuery} from '@/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/types/types';
import {PlusIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate} from 'react-router-dom';

const workflowTestApi = new WorkflowTestApi();

const ProjectHeader = ({
    bottomResizablePanelRef,
    projectId,
    projectWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    projectId: number;
    projectWorkflowId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) => {
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);

    const {
        setShowBottomPanelOpen,
        setShowEditWorkflowDialog,
        setWorkflowIsRunning,
        setWorkflowTestExecution,
        showEditWorkflowDialog,
        workflowIsRunning,
    } = useWorkflowEditorStore();

    const {setWorkflow, workflow} = useWorkflowDataStore();

    const {setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const navigate = useNavigate();

    const {componentNames, nodeNames} = workflow;

    const {data: project} = useGetProjectQuery(
        projectId,
        useLoaderData() as ProjectModel,
        !showDeleteProjectAlertDialog
    );

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
            });

            setShowBottomPanelOpen(false);
            setWorkflow({...workflow, componentNames, nodeNames});

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }

            navigate(`/automation/projects/${projectId}/project-workflows/${workflow.projectWorkflowId}`);
        },
    });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            navigate('/automation/projects');

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            setShowDeleteWorkflowAlertDialog(false);

            navigate('/automation/projects');

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const handleDeleteProjectAlertDialogClick = () => {
        if (projectId) {
            deleteProjectMutation.mutate(projectId);
        }
    };

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (projectId && workflow.id) {
            deleteWorkflowMutation.mutate({
                id: workflow.id!,
            });
        }
    };

    const handleProjectWorkflowValueChange = (projectWorkflowId: number) => {
        setWorkflowTestExecution(undefined);

        setCurrentNode(undefined);

        navigate(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}`);
    };

    const handleRunClick = () => {
        setShowBottomPanelOpen(true);
        setWorkflowIsRunning(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow.id) {
            workflowTestApi
                .testWorkflow({
                    id: workflow.id,
                })
                .then((workflowTestExecution) => {
                    setWorkflowTestExecution(workflowTestExecution);
                    setWorkflowIsRunning(false);

                    if (bottomResizablePanelRef.current && bottomResizablePanelRef.current.getSize() === 0) {
                        bottomResizablePanelRef.current.resize(35);
                    }
                })
                .catch(() => {
                    setWorkflowIsRunning(false);
                    setWorkflowTestExecution(undefined);
                });
        }
    };

    return (
        <header className="flex items-center border-b py-2 pl-3 pr-2.5">
            <div className="flex flex-1 items-center">
                <h1>{project?.name}</h1>

                {project && <ProjectHeaderProjectVersionBadge project={project} />}
            </div>

            <div className="flex items-center space-x-6">
                <div className="flex space-x-1">
                    <ProjectHeaderWorkflowSelect
                        onValueChange={handleProjectWorkflowValueChange}
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
                    />

                    <ProjectHeaderWorkflowDropDownMenu
                        onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                        projectId={projectId}
                        workflowId={workflow.id!}
                    />

                    {!!projectId && (
                        <WorkflowDialog
                            createWorkflowMutation={createProjectWorkflowMutation}
                            parentId={projectId}
                            triggerNode={
                                <Button className="hover:bg-gray-200" size="icon" variant="ghost">
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <PlusIcon className="mx-2 size-5" />
                                        </TooltipTrigger>

                                        <TooltipContent>Create new workflow</TooltipContent>
                                    </Tooltip>
                                </Button>
                            }
                            updateWorkflowMutation={updateWorkflowMutation}
                            useGetWorkflowQuery={useGetWorkflowQuery}
                        />
                    )}

                    {workflowIsRunning ? (
                        <ProjectHeaderStopButton />
                    ) : (
                        <ProjectHeaderRunButton onRunClick={handleRunClick} runDisabled={runDisabled} />
                    )}

                    <ProjectHeaderOutputButton bottomResizablePanelRef={bottomResizablePanelRef} />
                </div>

                {project && (
                    <div className="flex space-x-1">
                        <ProjectPublishPopover project={project} />

                        <ProjectHeaderProjectDropDownMenu
                            onDelete={() => setShowDeleteProjectAlertDialog(true)}
                            onEdit={() => setShowEditProjectDialog(true)}
                            project={project}
                        />
                    </div>
                )}
            </div>

            {showDeleteProjectAlertDialog && (
                <ProjectHeaderDeleteProjectAlertDialog
                    onClose={() => setShowDeleteProjectAlertDialog(false)}
                    onDelete={handleDeleteProjectAlertDialogClick}
                />
            )}

            {showDeleteWorkflowAlertDialog && (
                <ProjectHeaderDeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={handleDeleteWorkflowAlertDialogClick}
                />
            )}

            {showEditProjectDialog && project && (
                <ProjectDialog onClose={() => setShowEditProjectDialog(false)} project={project} />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </header>
    );
};

export default ProjectHeader;
