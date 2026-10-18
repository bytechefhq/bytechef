import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import updateCachedProjectWorkflowVersion from '@/pages/automation/project/utils/updateCachedProjectWorkflowVersion';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {RequestI, WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore, {
    clearWorkflowHistory,
    setWorkflowWithoutHistory,
} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {DEFAULT_CANVAS_WIDTH} from '@/shared/constants';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {PlatformType} from '@/shared/middleware/graphql';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {useGetComponentDefinitionsQuery as useGetComponentDefinitionsBaseQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {ProjectWorkflowKeys, useGetProjectWorkflowQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowNodeDescriptionKeys} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {WorkflowNodeParameterKeys} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useShallow} from 'zustand/react/shallow';

export interface EmbeddableWorkflowEditorPropsI {
    onWorkflowChange?: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    showPublishDeploy?: boolean;
    showWorkflowSelect?: boolean;
}

/**
 * A self-contained workflow editor suitable for embedding inside panels such as the AI Hub resource panel.
 *
 * It reads the module-level singleton workflow stores shared with the full Project page, so only one editor
 * (embedded or not) may be mounted at a time.
 *
 * Differences from the full Project page editor:
 * - No project left sidebar (the project/workflow tree)
 * - No Copilot panel (the AI Hub chat is the copilot)
 */
const EmbeddableWorkflowEditor = ({
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    showPublishDeploy,
    showWorkflowSelect,
}: EmbeddableWorkflowEditorPropsI) => {
    const [canvasWidth, setCanvasWidth] = useState(DEFAULT_CANVAS_WIDTH);

    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);
    const canvasContainerRef = useRef<HTMLDivElement>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const {setIsWorkflowLoaded, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setIsWorkflowLoaded: state.setIsWorkflowLoaded,
            workflow: state.workflow,
        }))
    );
    const setShowBottomPanelOpen = useWorkflowEditorStore((state) => state.setShowBottomPanelOpen);

    const queryClient = useQueryClient();

    const {data: currentWorkflow, isLoading: isWorkflowLoading} = useGetProjectWorkflowQuery(
        projectId,
        projectWorkflowId,
        projectId > 0 && projectWorkflowId > 0,
        undefined,
        {staleTime: 30_000}
    );

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation({
        onSuccess: (result, variables) =>
            updateCachedProjectWorkflowVersion(queryClient, {
                projectId,
                version: result.version,
                workflowId: variables.id,
            }),
    });

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: (result, variables) =>
            updateCachedProjectWorkflowVersion(queryClient, {
                projectId,
                version: result.version,
                workflowId: variables.id,
            }),
    });

    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation({
        onSuccess: (result, variables) =>
            updateCachedProjectWorkflowVersion(queryClient, {
                projectId,
                version: result.version,
                workflowId: variables.id,
            }),
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: (updatedWorkflow) =>
            updateCachedProjectWorkflowVersion(queryClient, {
                projectId,
                version: updatedWorkflow.version,
                workflowId: updatedWorkflow.id,
            }),
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: (result, variables) => {
            updateCachedProjectWorkflowVersion(queryClient, {
                projectId,
                version: result.version,
                workflowId: variables.id,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeDescriptionKeys.workflowNodeDescription({
                    environmentId: variables.environmentId,
                    id: variables.id,
                    workflowNodeName: variables.workflowNodeName,
                }),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions({
                    environmentId: variables.environmentId,
                    id: variables.id,
                    workflowNodeName: variables.workflowNodeName,
                }),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOutputKeys.workflowNodeOutputs,
            });
        },
    });

    const {runDisabled} = useRun();

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const useGetComponentDefinitionsQuery = (request: GetComponentDefinitionsRequestI, enabled?: boolean) =>
        useGetComponentDefinitionsBaseQuery(request, enabled);

    const useGetConnectionsQuery = (request: RequestI, enabled?: boolean) =>
        useGetWorkspaceConnectionsQuery(
            {
                environmentId: currentEnvironmentId,
                id: currentWorkspaceId!,
                ...request,
            },
            enabled
        );

    const cancelWorkflowQueries = () =>
        queryClient.cancelQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});

    const invalidateWorkflowQueries = () =>
        queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});

    const chatTrigger =
        workflow.triggers != null && workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1;

    const handleTestOutputCloseClick = () => {
        setShowBottomPanelOpen(false);

        bottomResizablePanelRef.current?.resize(0);
    };

    useEffect(() => {
        return () => {
            setWorkflowWithoutHistory({}, {clearHistory: true});
        };
    }, []);

    useEffect(() => {
        setIsWorkflowLoaded(false);

        clearWorkflowHistory();
    }, [projectWorkflowId, setIsWorkflowLoaded]);

    useEffect(() => {
        useDataPillPanelStore.getState().setDataPillPanelOpen(false);
        useWorkflowTestChatStore.getState().setWorkflowTestChatPanelOpen(false);

        useWorkflowNodeDetailsPanelStore.getState().reset();

        useWorkflowNodeDetailsPanelStore.getState().clearPendingSaveNodeNames();
    }, [projectWorkflowId]);

    useEffect(() => {
        const containerElement = canvasContainerRef.current;

        if (!containerElement) {
            return;
        }

        const updateCanvasWidth = () => setCanvasWidth(containerElement.clientWidth);

        updateCanvasWidth();

        const resizeObserver = new ResizeObserver(updateCanvasWidth);

        resizeObserver.observe(containerElement);

        return () => resizeObserver.disconnect();
    }, []);

    useEffect(() => {
        if (currentWorkflow && !isWorkflowLoading) {
            const timeoutId = setTimeout(() => {
                setWorkflowWithoutHistory({...currentWorkflow});
            }, 0);

            return () => clearTimeout(timeoutId);
        }
    }, [currentWorkflow, isWorkflowLoading]);

    return (
        <>
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            <WorkflowEditorProvider
                value={{
                    ConnectionKeys: ConnectionKeys,
                    cancelWorkflowQueries,
                    deleteClusterElementParameterMutation,
                    deleteWorkflowNodeParameterMutation,
                    invalidateWorkflowQueries,
                    platformType: PlatformType.Automation,
                    updateClusterElementParameterMutation,
                    updateWorkflowMutation: updateWorkflowEditorMutation,
                    updateWorkflowNodeParameterMutation,
                    useCreateConnectionMutation: useCreateConnectionMutation,
                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                    useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                    useGetConnectionsQuery,
                    webhookTriggerTestApi: new WebhookTriggerTestApi(),
                }}
            >
                <div className="flex size-full flex-col">
                    <ProjectHeader
                        bottomResizablePanelRef={bottomResizablePanelRef}
                        chatTrigger={chatTrigger}
                        embedded
                        onWorkflowChange={onWorkflowChange}
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
                        runDisabled={runDisabled}
                        showPublishDeploy={showPublishDeploy}
                        showWorkflowSelect={showWorkflowSelect}
                        updateWorkflowMutation={updateWorkflowEditorMutation}
                    />

                    <ResizablePanelGroup className="flex-1 bg-surface-main" orientation="vertical">
                        <ResizablePanel className="relative flex" defaultSize={650}>
                            <div className="flex size-full" ref={canvasContainerRef}>
                                <WorkflowEditorLayout
                                    customCanvasWidth={canvasWidth}
                                    fitViewOnWorkflowChange
                                    leftSidebarOpen={false}
                                    runDisabled={runDisabled}
                                    showCopilot={false}
                                    showWorkflowInputs={true}
                                />
                            </div>
                        </ResizablePanel>

                        <ResizableHandle className="bg-transparent aria-[orientation=horizontal]:-top-1.5 aria-[orientation=horizontal]:h-0" />

                        <ResizablePanel className="flex" defaultSize={0} panelRef={bottomResizablePanelRef}>
                            {workflowTestExecution && (
                                <div className="mx-3 mb-3 flex flex-1 overflow-hidden rounded-lg bg-background">
                                    <WorkflowExecutionsTestOutput
                                        onCloseClick={handleTestOutputCloseClick}
                                        workflowIsRunning={workflowIsRunning}
                                        workflowTestExecution={workflowTestExecution}
                                    />
                                </div>
                            )}
                        </ResizablePanel>
                    </ResizablePanelGroup>
                </div>
            </WorkflowEditorProvider>
        </>
    );
};

export default EmbeddableWorkflowEditor;
