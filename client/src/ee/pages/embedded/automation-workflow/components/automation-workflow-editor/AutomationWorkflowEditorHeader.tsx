import {Separator} from '@/components/ui/separator';
import {Skeleton} from '@/components/ui/skeleton';
import AutomationWorkflowEditorBreadcrumb from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorBreadcrumb';
import AutomationWorkflowEditorSettingsMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorSettingsMenu';
import AutomationWorkflowProjectVersionHistorySheet from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowProjectVersionHistorySheet';
import LeftSidebarButton from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/LeftSidebarButton';
import OutputButton from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/OutputButton';
import PublishPopover from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/PublishPopover';
import WorkflowActionsButton from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/WorkflowActionsButton';
import {useAutomationWorkflowEditorHeader} from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/hooks/useAutomationWorkflowEditorHeader';
import {useAutomationWorkflowEditorSidebarStore} from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/stores/useAutomationWorkflowEditorSidebarStore';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog';
import AutomationWorkflowProjectDialog, {
    AutomationWorkflowProjectFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog';
import DeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteProjectAlertDialog';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {
    useAutomationWorkflowProjectCategoriesQuery,
    useAutomationWorkflowProjectTagsQuery,
    useAutomationWorkflowProjectsQuery,
    useDeleteAutomationWorkflowProjectMutation,
    useDeleteAutomationWorkflowProjectWorkflowMutation,
    useDuplicateAutomationWorkflowProjectMutation,
    useDuplicateAutomationWorkflowProjectWorkflowMutation,
    useUpdateAutomationWorkflowProjectMutation,
} from '@/shared/middleware/graphql';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsFetching, useQueryClient} from '@tanstack/react-query';
import {RefObject, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface AutomationWorkflowEditorHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    currentWorkflowId: string;
    projectId: string;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const AutomationWorkflowEditorHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    currentWorkflowId,
    projectId,
    runDisabled,
    updateWorkflowMutation,
}: AutomationWorkflowEditorHeaderProps) => {
    const [showProjectDeleteAlert, setShowProjectDeleteAlert] = useState(false);
    const [showProjectEditDialog, setShowProjectEditDialog] = useState(false);
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowDeleteAlert, setShowWorkflowDeleteAlert] = useState(false);
    const [showWorkflowEditDialog, setShowWorkflowEditDialog] = useState(false);

    const copilotLayoutShifted = useCopilotLayoutShifted();
    const {leftSidebarOpen, setLeftSidebarOpen} = useAutomationWorkflowEditorSidebarStore(
        useShallow((state) => ({
            leftSidebarOpen: state.leftSidebarOpen,
            setLeftSidebarOpen: state.setLeftSidebarOpen,
        }))
    );
    const {workflowIsRunning} = useWorkflowEditorStore(
        useShallow((state) => ({
            workflowIsRunning: state.workflowIsRunning,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const isFetching = useIsFetching();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        handleWorkflowValueChange,
        project,
        publishProjectMutationIsPending,
    } = useAutomationWorkflowEditorHeader({
        bottomResizablePanelRef,
        projectId,
    });

    useAutomationWorkflowProjectsQuery();

    const {data: categoriesData} = useAutomationWorkflowProjectCategoriesQuery();
    const {data: tagsData} = useAutomationWorkflowProjectTagsQuery();

    const categories = categoriesData?.automationWorkflowProjectCategories;
    const tags = tagsData?.automationWorkflowProjectTags;

    const deleteWorkflowMutation = useDeleteAutomationWorkflowProjectWorkflowMutation();
    const deleteProjectMutation = useDeleteAutomationWorkflowProjectMutation();
    const duplicateWorkflowMutation = useDuplicateAutomationWorkflowProjectWorkflowMutation();
    const duplicateProjectMutation = useDuplicateAutomationWorkflowProjectMutation();
    const updateProjectMutation = useUpdateAutomationWorkflowProjectMutation();

    const isOnline = onlineManager.isOnline();

    const handleDuplicateWorkflowClick = () => {
        duplicateWorkflowMutation.mutate(
            {workflowUuid: currentWorkflowId},
            {
                onSuccess: (data) => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    navigate(
                        '/embedded/automation-workflows/' + data.duplicateAutomationWorkflowProjectWorkflow + '/editor'
                    );
                },
            }
        );
    };

    const handleDuplicateProjectClick = () => {
        if (!project?.id) {
            return;
        }

        duplicateProjectMutation.mutate(
            {id: project.id},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    navigate('/embedded/automation-workflows');
                },
            }
        );
    };

    const handleProjectHistoryClick = () => {
        setShowProjectVersionHistorySheet(true);
    };

    const handleEditWorkflowClick = () => {
        setShowWorkflowEditDialog(true);
    };

    const parseWorkflowDefinition = (): Record<string, unknown> => {
        try {
            return JSON.parse(workflow.definition ?? '{}') as Record<string, unknown>;
        } catch {
            return {};
        }
    };

    const handleEditWorkflowSubmit = (values: AutomationWorkflowFormValuesI) => {
        if (!workflow.id) {
            return;
        }

        const existingDefinition = parseWorkflowDefinition();

        updateWorkflowMutation.mutate(
            {
                id: workflow.id,
                workflow: {
                    definition: JSON.stringify({
                        ...existingDefinition,
                        description: values.description,
                        label: values.label,
                    }),
                    version: workflow.version,
                },
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
                    queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

                    setShowWorkflowEditDialog(false);
                },
            }
        );
    };

    const handleExportWorkflowClick = () => {
        window.location.href = '/api/automation/internal/workflows/' + workflow.id + '/export';
    };

    const handleDeleteWorkflowClick = () => {
        setShowWorkflowDeleteAlert(true);
    };

    const handleConfirmDeleteWorkflow = () => {
        deleteWorkflowMutation.mutate(
            {workflowUuid: currentWorkflowId},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    navigate('/embedded/automation-workflows');
                },
            }
        );
    };

    const handleEditProjectClick = () => {
        setShowProjectEditDialog(true);
    };

    const handleEditProjectSubmit = (values: AutomationWorkflowProjectFormValuesI) => {
        if (!project?.id) {
            return;
        }

        updateProjectMutation.mutate(
            {
                category: values.category || undefined,
                description: values.description || undefined,
                id: project.id,
                name: values.name,
                tags: values.tags,
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCategories']});
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectTags']});

                    setShowProjectEditDialog(false);
                },
            }
        );
    };

    const handleExportProjectClick = () => {
        window.location.href = '/api/automation/internal/projects/' + project?.id + '/export';
    };

    const handleDeleteProjectClick = () => {
        setShowProjectDeleteAlert(true);
    };

    const handleConfirmDeleteProject = () => {
        if (!project?.id) {
            return;
        }

        deleteProjectMutation.mutate(
            {id: project.id},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    navigate('/embedded/automation-workflows');
                },
            }
        );
    };

    if (!project) {
        return (
            <header className="flex items-center justify-between bg-surface-main px-3 py-2.5">
                <div className="flex items-center gap-5">
                    <Skeleton className="h-6 w-80" />
                </div>

                <div className="flex items-center gap-4">
                    <Skeleton className="size-6" />

                    <Skeleton className="size-4 rounded-full" />

                    <Skeleton className="size-6" />

                    <div className="flex gap-2">
                        <Skeleton className="h-9 w-28" />

                        <Skeleton className="h-9 w-20" />
                    </div>
                </div>
            </header>
        );
    }

    return (
        <header
            className={twMerge(
                'flex items-center justify-between bg-surface-main px-3 py-2.5',
                leftSidebarOpen && 'pl-0 pr-3',
                copilotLayoutShifted && 'pr-0'
            )}
        >
            <div className="flex items-center">
                <LeftSidebarButton onLeftSidebarOpenClick={() => setLeftSidebarOpen(!leftSidebarOpen)} />

                <Separator className="ml-2 mr-4 h-4" orientation="vertical" />

                <AutomationWorkflowEditorBreadcrumb
                    currentWorkflowId={currentWorkflowId}
                    onWorkflowValueChange={handleWorkflowValueChange}
                    project={project}
                />
            </div>

            <div className="flex items-center">
                <LoadingIndicator isFetching={isFetching} isOnline={isOnline} />

                <AutomationWorkflowEditorSettingsMenu
                    onDeleteProjectClick={handleDeleteProjectClick}
                    onDeleteWorkflowClick={handleDeleteWorkflowClick}
                    onDuplicateProjectClick={handleDuplicateProjectClick}
                    onDuplicateWorkflowClick={handleDuplicateWorkflowClick}
                    onEditProjectClick={handleEditProjectClick}
                    onEditWorkflowClick={handleEditWorkflowClick}
                    onExportProjectClick={handleExportProjectClick}
                    onExportWorkflowClick={handleExportWorkflowClick}
                    onProjectHistoryClick={handleProjectHistoryClick}
                />

                <OutputButton onShowOutputClick={handleShowOutputClick} />

                <WorkflowActionsButton
                    chatTrigger={chatTrigger ?? false}
                    onRunClick={handleRunClick}
                    onStopClick={handleStopClick}
                    runDisabled={runDisabled}
                    workflowIsRunning={workflowIsRunning}
                />

                <PublishPopover
                    isPending={publishProjectMutationIsPending}
                    onPublishProjectSubmit={handlePublishProjectSubmit}
                />
            </div>

            {showWorkflowEditDialog && (
                <AutomationWorkflowDialog
                    onClose={() => setShowWorkflowEditDialog(false)}
                    onSubmit={handleEditWorkflowSubmit}
                    workflow={workflow}
                />
            )}

            {showProjectEditDialog && (
                <AutomationWorkflowProjectDialog
                    categories={categories}
                    onClose={() => setShowProjectEditDialog(false)}
                    onSubmit={handleEditProjectSubmit}
                    project={project}
                    tags={tags}
                />
            )}

            {showWorkflowDeleteAlert && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowWorkflowDeleteAlert(false)}
                    onDelete={() => {
                        handleConfirmDeleteWorkflow();

                        setShowWorkflowDeleteAlert(false);
                    }}
                />
            )}

            {showProjectDeleteAlert && (
                <DeleteProjectAlertDialog
                    onClose={() => setShowProjectDeleteAlert(false)}
                    onDelete={() => {
                        handleConfirmDeleteProject();

                        setShowProjectDeleteAlert(false);
                    }}
                />
            )}

            {project && (
                <AutomationWorkflowProjectVersionHistorySheet
                    onClose={() => setShowProjectVersionHistorySheet(false)}
                    open={showProjectVersionHistorySheet}
                    projectId={project.id}
                />
            )}
        </header>
    );
};

export default AutomationWorkflowEditorHeader;
