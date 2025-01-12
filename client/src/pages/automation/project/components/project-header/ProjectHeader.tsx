import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectHeaderDeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/ProjectHeaderDeleteProjectAlertDialog';
import ProjectHeaderDeleteWorkflowAlertDialog from '@/pages/automation/project/components/project-header/ProjectHeaderDeleteWorkflowAlertDialog';
import ProjectHeaderOutputButton from '@/pages/automation/project/components/project-header/ProjectHeaderOutputButton';
import ProjectHeaderProjectDropDownMenu from '@/pages/automation/project/components/project-header/ProjectHeaderProjectDropDownMenu';
import ProjectHeaderPublishPopover from '@/pages/automation/project/components/project-header/ProjectHeaderPublishPopover';
import ProjectHeaderRunButton from '@/pages/automation/project/components/project-header/ProjectHeaderRunButton';
import ProjectHeaderStopButton from '@/pages/automation/project/components/project-header/ProjectHeaderStopButton';
import ProjectHeaderTitle from '@/pages/automation/project/components/project-header/ProjectHeaderTitle';
import ProjectHeaderWorkflowDropDownMenu from '@/pages/automation/project/components/project-header/ProjectHeaderWorkflowDropDownMenu';
import ProjectHeaderWorkflowSelect from '@/pages/automation/project/components/project-header/ProjectHeaderWorkflowSelect';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useDeleteProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {
    useCreateProjectWorkflowMutation,
    useDeleteWorkflowMutation,
} from '@/shared/mutations/automation/workflows.mutations';
import {ProjectCategoryKeys} from '@/shared/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/shared/queries/automation/projectTags.queries';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {PlusIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {RefObject, useCallback, useEffect, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useSearchParams} from 'react-router-dom';

const workflowTestApi = new WorkflowTestApi();

const ProjectHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    projectId,
    projectWorkflowId,
    runDisabled,
    updateWorkflowMutation,
}: {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    chatTrigger?: boolean;
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
    const {workflow} = useWorkflowDataStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {setCurrentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {resetMessages, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} = useWorkflowTestChatStore();

    const {captureProjectWorkflowCreated, captureProjectWorkflowTested} = useAnalytics();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const {data: project} = useGetProjectQuery(projectId, useLoaderData() as Project, !showDeleteProjectAlertDialog);

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (projectWorkflowId) => {
            captureProjectWorkflowCreated();

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
            });

            setShowBottomPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }

            navigate(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}`);
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

            navigate(
                `/automation/projects/${projectId}/project-workflows/${project?.projectWorkflowIds?.filter((projectWorkflowId) => projectWorkflowId !== (workflow as Workflow).projectWorkflowId)[0]}?${searchParams}`
            );

            queryClient.removeQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(projectId, (workflow as Workflow).projectWorkflowId!),
            });
            queryClient.removeQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const handleDeleteProjectAlertDialogClick = () => {
        if (projectId) {
            deleteProjectMutation.mutate(projectId);

            navigate('/automation/projects');
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

        navigate(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}?${searchParams}`);
    };

    const handleRunClick = () => {
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow.id) {
            captureProjectWorkflowTested();

            if (chatTrigger) {
                resetMessages();
                setDataPillPanelOpen(false);
                setWorkflowNodeDetailsPanelOpen(false);
                setWorkflowTestChatPanelOpen(true);
            } else {
                setWorkflowIsRunning(true);

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
        }
    };

    const handleStopClick = useCallback(() => {
        setWorkflowIsRunning(false);

        if (chatTrigger) {
            setWorkflowTestChatPanelOpen(false);

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }
        }
    }, [bottomResizablePanelRef, chatTrigger, setWorkflowIsRunning, setWorkflowTestChatPanelOpen]);

    useEffect(() => {
        if (workflowNodeDetailsPanelOpen || !workflowTestChatPanelOpen) {
            handleStopClick();
        }
    }, [handleStopClick, workflowNodeDetailsPanelOpen, workflowTestChatPanelOpen]);

    return (
        <header className="flex items-center border-b border-b-border/50 py-2.5 pl-3 pr-2.5">
            <div className="flex flex-1">{project && <ProjectHeaderTitle project={project} />}</div>

            <div className="flex items-center space-x-12">
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
                                <Button className="hover:bg-background/70 [&_svg]:size-5" size="icon" variant="ghost">
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <PlusIcon />
                                        </TooltipTrigger>

                                        <TooltipContent>New workflow</TooltipContent>
                                    </Tooltip>
                                </Button>
                            }
                            updateWorkflowMutation={updateWorkflowMutation}
                            useGetWorkflowQuery={useGetWorkflowQuery}
                        />
                    )}

                    {workflowIsRunning ? (
                        <ProjectHeaderStopButton onStopClick={handleStopClick} />
                    ) : (
                        <ProjectHeaderRunButton
                            chatTrigger={chatTrigger ?? false}
                            onRunClick={handleRunClick}
                            runDisabled={runDisabled}
                        />
                    )}

                    <ProjectHeaderOutputButton bottomResizablePanelRef={bottomResizablePanelRef} />
                </div>

                {project && (
                    <div className="flex space-x-1">
                        <ProjectHeaderPublishPopover project={project} />

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
