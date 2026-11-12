import AutomationEditorBreadcrumb from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorBreadcrumb';
import AutomationEditorSettingsMenu from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorSettingsMenu';
import AutomationEditorWorkflowSelect from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorWorkflowSelect';
import AutomationProjectVersionHistorySheet from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationProjectVersionHistorySheet';
import OutputButton from '@/ee/pages/embedded/automation/components/automation-editor/components/OutputButton';
import PublishPopover from '@/ee/pages/embedded/automation/components/automation-editor/components/PublishPopover';
import {useAutomationEditorHeader} from '@/ee/pages/embedded/automation/components/automation-editor/hooks/useAutomationEditorHeader';
import {useAutomationEditorSidebarStore} from '@/ee/pages/embedded/automation/components/automation-editor/stores/useAutomationEditorSidebarStore';
import {useCreateAutomationProjectWorkflow} from '@/ee/pages/embedded/automation/hooks/useCreateAutomationProjectWorkflow';
import AutomationDialog, {
    AutomationFormValuesI,
} from '@/ee/pages/embedded/automations/components/automation-dialog/AutomationDialog';
import AutomationProjectDialog, {
    AutomationProjectFormValuesI,
} from '@/ee/pages/embedded/automations/components/automation-project-dialog/AutomationProjectDialog';
import ProjectSkeleton from '@/pages/automation/project/components/project-header/components/ProjectSkeleton';
import DeleteProjectAlertDialog from '@/pages/automation/project/components/project-header/components/settings-menu/components/DeleteProjectAlertDialog';
import WorkflowActionsButton from '@/pages/platform/workflow-editor/components/WorkflowActionsButton';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import LoadingIndicator from '@/shared/components/LoadingIndicator';
import useCopilotCompactHeader from '@/shared/components/copilot/hooks/useCopilotCompactHeader';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import LeftSidebarButton from '@/shared/layout/LeftSidebarButton';
import {
    useAutomationProjectCategoriesQuery,
    useAutomationProjectTagsQuery,
    useDeleteAutomationProjectMutation,
    useDeleteAutomationProjectWorkflowMutation,
    useDuplicateAutomationProjectMutation,
    useDuplicateAutomationProjectWorkflowMutation,
    useUpdateAutomationProjectMutation,
    useUpdateAutomationProjectWorkflowPermissionExpressionMutation,
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

interface AutomationEditorHeaderProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    chatTrigger?: boolean;
    currentWorkflowId: string;
    projectId: string;
    runDisabled: boolean;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const AutomationEditorHeader = ({
    bottomResizablePanelRef,
    chatTrigger,
    currentWorkflowId,
    projectId,
    runDisabled,
    updateWorkflowMutation,
}: AutomationEditorHeaderProps) => {
    const [showCreateWorkflowDialog, setShowCreateWorkflowDialog] = useState(false);
    const [showProjectDeleteAlert, setShowProjectDeleteAlert] = useState(false);
    const [showProjectEditDialog, setShowProjectEditDialog] = useState(false);
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowDeleteAlert, setShowWorkflowDeleteAlert] = useState(false);
    const [showWorkflowEditDialog, setShowWorkflowEditDialog] = useState(false);

    const {leftSidebarOpen, setLeftSidebarOpen} = useAutomationEditorSidebarStore(
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

    useCopilotCompactHeader();
    const isOnline = useSyncExternalStore(subscribeToOnlineStatus, getOnlineStatus);
    const isSaving = useIsMutating();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {
        handleProjectValueChange,
        handlePublishProjectSubmit,
        handleRunClick,
        handleShowOutputClick,
        handleStopClick,
        handleWorkflowValueChange,
        project,
        projects,
        publishProjectMutationIsPending,
    } = useAutomationEditorHeader({
        bottomResizablePanelRef,
        projectId,
    });

    const {createWorkflow, handleWorkflowFileChange, workflowFileInputRef} = useCreateAutomationProjectWorkflow({
        bottomResizablePanelRef,
        projectId: project?.id,
    });

    const {data: categoriesData} = useAutomationProjectCategoriesQuery();
    const {data: tagsData} = useAutomationProjectTagsQuery();

    const categories = categoriesData?.automationProjectCategories;
    const tags = tagsData?.automationProjectTags;

    const deleteWorkflowMutation = useDeleteAutomationProjectWorkflowMutation();
    const deleteProjectMutation = useDeleteAutomationProjectMutation();
    const duplicateWorkflowMutation = useDuplicateAutomationProjectWorkflowMutation();
    const duplicateProjectMutation = useDuplicateAutomationProjectMutation();
    const updateProjectMutation = useUpdateAutomationProjectMutation();
    const updateWorkflowPermissionExpressionMutation = useUpdateAutomationProjectWorkflowPermissionExpressionMutation();

    const currentWorkflowTemplate = project?.workflowTemplates.find(
        (workflowTemplate) => workflowTemplate.workflowUuid === currentWorkflowId
    );

    const loadingIndicator = (isSaving > 0 || !isOnline) && (
        <LoadingIndicator className="shrink-0" isFetching={isSaving} isOnline={isOnline} showLabel />
    );

    const handleDuplicateWorkflowClick = () => {
        duplicateWorkflowMutation.mutate(
            {workflowUuid: currentWorkflowId},
            {
                onSuccess: (data) => {
                    void queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    void navigate('/embedded/automations/' + data.duplicateAutomationProjectWorkflow + '/editor');
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
                    void queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    void navigate('/embedded/automations');
                },
            }
        );
    };

    const handleCreateWorkflowSubmit = (values: AutomationFormValuesI) => {
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

    const handleEditWorkflowSubmit = (values: AutomationFormValuesI) => {
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
                    void queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflow.id!)});

                    updateWorkflowPermissionExpressionMutation.mutate(
                        {permissionExpression: values.permissionExpression, workflowUuid: currentWorkflowId},
                        {
                            onSuccess: () => {
                                void queryClient.invalidateQueries({queryKey: ['automationProjects']});

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
                    void queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    void navigate('/embedded/automations');
                },
            }
        );
    };

    const handleEditProjectClick = () => {
        setShowProjectEditDialog(true);
    };

    const handleEditProjectSubmit = (values: AutomationProjectFormValuesI) => {
        if (!project?.id) {
            return;
        }

        updateProjectMutation.mutate(
            {
                category: values.category || undefined,
                description: values.description || undefined,
                id: project.id,
                name: values.name,
                permissionExpression: values.permissionExpression,
                tags: values.tags,
            },
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['automationProjects']});
                    void queryClient.invalidateQueries({queryKey: ['automationProjectCategories']});
                    void queryClient.invalidateQueries({queryKey: ['automationProjectTags']});

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
                    void queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    void navigate('/embedded/automations');
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
            <div className="flex min-w-0 items-center gap-2">
                <LeftSidebarButton
                    onLeftSidebarOpenClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                    tooltip="See workflows"
                />

                <AutomationEditorBreadcrumb
                    itemSelect={
                        <AutomationEditorWorkflowSelect
                            currentWorkflowId={currentWorkflowId}
                            onValueChange={handleWorkflowValueChange}
                            workflows={project.workflowTemplates}
                        />
                    }
                    onProjectValueChange={handleProjectValueChange}
                    project={project}
                    projects={projects}
                />

                {loadingIndicator}
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

                <AutomationEditorSettingsMenu
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
                <AutomationDialog
                    onClose={() => setShowCreateWorkflowDialog(false)}
                    onSubmit={handleCreateWorkflowSubmit}
                />
            )}

            {showWorkflowEditDialog && (
                <AutomationDialog
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
                <AutomationProjectDialog
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
                <AutomationProjectVersionHistorySheet
                    onClose={() => setShowProjectVersionHistorySheet(false)}
                    open={showProjectVersionHistorySheet}
                    projectId={project.id}
                />
            )}
        </header>
    );
};

export default AutomationEditorHeader;
