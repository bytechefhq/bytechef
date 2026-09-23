import AutomationWorkflowEditorBreadcrumb from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorBreadcrumb';
import AutomationWorkflowEditorSettingsMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorSettingsMenu';
import AutomationWorkflowEditorWorkflowSelect from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowSelect';
import AutomationWorkflowProjectVersionHistorySheet from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowProjectVersionHistorySheet';
import LeftSidebarButton from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/LeftSidebarButton';
import OutputButton from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/OutputButton';
import PublishPopover from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/PublishPopover';
import WorkflowActionsButton from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/WorkflowActionsButton';
import {useAutomationWorkflowEditorHeader} from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/hooks/useAutomationWorkflowEditorHeader';
import {useAutomationWorkflowEditorSidebarStore} from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/stores/useAutomationWorkflowEditorSidebarStore';
import {useCreateAutomationWorkflowProjectWorkflow} from '@/ee/pages/embedded/automation-workflow/hooks/useCreateAutomationWorkflowProjectWorkflow';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog';
import AutomationWorkflowProjectDialog, {
    AutomationWorkflowProjectFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog';
import ProjectSkeleton from '@/pages/automation/project/components/project-header/components/ProjectSkeleton';
import DeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteProjectAlertDialog';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {
    useAutomationWorkflowProjectCategoriesQuery,
    useAutomationWorkflowProjectTagsQuery,
    useDeleteAutomationWorkflowProjectMutation,
    useDeleteAutomationWorkflowProjectWorkflowMutation,
    useDuplicateAutomationWorkflowProjectMutation,
    useDuplicateAutomationWorkflowProjectWorkflowMutation,
    useUpdateAutomationWorkflowProjectMutation,
    useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation,
} from '@/shared/middleware/graphql';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {onlineManager, useIsMutating, useQueryClient} from '@tanstack/react-query';
import {RefObject, useState, useSyncExternalStore} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const getOnlineStatus = () => onlineManager.isOnline();

const subscribeToOnlineStatus = (onOnlineStatusChange: () => void) => onlineManager.subscribe(onOnlineStatusChange);

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
    const [showCreateWorkflowDialog, setShowCreateWorkflowDialog] = useState(false);
    const [showProjectDeleteAlert, setShowProjectDeleteAlert] = useState(false);
    const [showProjectEditDialog, setShowProjectEditDialog] = useState(false);
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowDeleteAlert, setShowWorkflowDeleteAlert] = useState(false);
    const [showWorkflowEditDialog, setShowWorkflowEditDialog] = useState(false);

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

    const copilotLayoutShifted = useCopilotLayoutShifted();
    const isOnline = useSyncExternalStore(subscribeToOnlineStatus, getOnlineStatus);
    const isSaving = useIsMutating();
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

    const {createWorkflow, handleWorkflowFileChange, workflowFileInputRef} = useCreateAutomationWorkflowProjectWorkflow(
        {
            bottomResizablePanelRef,
            projectId: project?.id,
        }
    );

    const {data: categoriesData} = useAutomationWorkflowProjectCategoriesQuery();
    const {data: tagsData} = useAutomationWorkflowProjectTagsQuery();

    const categories = categoriesData?.automationWorkflowProjectCategories;
    const tags = tagsData?.automationWorkflowProjectTags;

    const deleteWorkflowMutation = useDeleteAutomationWorkflowProjectWorkflowMutation();
    const deleteProjectMutation = useDeleteAutomationWorkflowProjectMutation();
    const duplicateWorkflowMutation = useDuplicateAutomationWorkflowProjectWorkflowMutation();
    const duplicateProjectMutation = useDuplicateAutomationWorkflowProjectMutation();
    const updateProjectMutation = useUpdateAutomationWorkflowProjectMutation();
    const updateWorkflowPermissionExpressionMutation =
        useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation();

    const currentWorkflowTemplate = project?.workflowTemplates.find(
        (workflowTemplate) => workflowTemplate.workflowUuid === currentWorkflowId
    );

    const loadingIndicator = (isSaving > 0 || !isOnline) && (
        <LoadingIndicator
            className="absolute -top-1 -right-1 size-5 rounded-full"
            isFetching={isSaving}
            isOnline={isOnline}
        />
    );

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

    const handleCreateWorkflowSubmit = (values: AutomationWorkflowFormValuesI) => {
        createWorkflow(values);

        setShowCreateWorkflowDialog(false);
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
                    queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

                    updateWorkflowPermissionExpressionMutation.mutate(
                        {permissionExpression: values.permissionExpression, workflowUuid: currentWorkflowId},
                        {
                            onSuccess: () => {
                                queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                                setShowWorkflowEditDialog(false);
                            },
                        }
                    );
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
                automationHubVisible: values.automationHubVisible,
                category: values.category || undefined,
                description: values.description || undefined,
                id: project.id,
                name: values.name,
                permissionExpression: values.permissionExpression,
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
        return <ProjectSkeleton />;
    }

    return (
        <header
            className={twMerge(
                'flex items-center justify-between bg-surface-main px-3 py-2.5 transition-[padding] duration-300 ease-in-out',
                leftSidebarOpen && 'pr-3 pl-0',
                copilotLayoutShifted && 'pr-0'
            )}
        >
            <div className="flex items-center gap-2">
                <LeftSidebarButton onLeftSidebarOpenClick={() => setLeftSidebarOpen(!leftSidebarOpen)} />

                <AutomationWorkflowEditorBreadcrumb
                    itemSelect={
                        <AutomationWorkflowEditorWorkflowSelect
                            currentWorkflowId={currentWorkflowId}
                            onValueChange={handleWorkflowValueChange}
                            workflows={project.workflowTemplates}
                        />
                    }
                    project={project}
                />
            </div>

            <div className="flex items-center gap-1">
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

                <OutputButton onShowOutputClick={handleShowOutputClick} />

                <div className="relative">
                    <AutomationWorkflowEditorSettingsMenu
                        onDeleteProjectClick={handleDeleteProjectClick}
                        onDeleteWorkflowClick={handleDeleteWorkflowClick}
                        onDuplicateProjectClick={handleDuplicateProjectClick}
                        onDuplicateWorkflowClick={handleDuplicateWorkflowClick}
                        onEditProjectClick={handleEditProjectClick}
                        onEditWorkflowClick={handleEditWorkflowClick}
                        onExportProjectClick={handleExportProjectClick}
                        onExportWorkflowClick={handleExportWorkflowClick}
                        onImportWorkflowClick={() => workflowFileInputRef.current?.click()}
                        onNewWorkflowClick={() => setShowCreateWorkflowDialog(true)}
                        onProjectHistoryClick={handleProjectHistoryClick}
                    />

                    {loadingIndicator}
                </div>
            </div>

            <input
                accept=".json,.yaml,.yml"
                aria-label="Import workflow file"
                className="hidden"
                onChange={handleWorkflowFileChange}
                ref={workflowFileInputRef}
                type="file"
            />

            {showCreateWorkflowDialog && (
                <AutomationWorkflowDialog
                    onClose={() => setShowCreateWorkflowDialog(false)}
                    onSubmit={handleCreateWorkflowSubmit}
                />
            )}

            {showWorkflowEditDialog && (
                <AutomationWorkflowDialog
                    onClose={() => setShowWorkflowEditDialog(false)}
                    onSubmit={handleEditWorkflowSubmit}
                    workflow={{
                        description: workflow.description,
                        label: workflow.label,
                        permissionExpression: currentWorkflowTemplate?.permissionExpression,
                    }}
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
