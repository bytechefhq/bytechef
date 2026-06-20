import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowDataStoreProvider} from '@/pages/platform/workflow-editor/providers/WorkflowDataStoreProvider';
import {RequestI, WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {DEFAULT_CANVAS_WIDTH} from '@/shared/constants';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
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

interface EmbeddableWorkflowEditorInnerPropsI {
    onWorkflowChange?: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
    showPublishDeploy?: boolean;
    showWorkflowSelect?: boolean;
}

/**
 * Inner component rendered inside WorkflowDataStoreProvider. Uses the module-level
 * singleton useWorkflowDataStore so all existing workflow-editor utility hooks work
 * without changes. The WorkflowDataStoreProvider wrapper provides store isolation.
 */
const EmbeddableWorkflowEditorInner = ({
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    showPublishDeploy,
    showWorkflowSelect,
}: EmbeddableWorkflowEditorInnerPropsI) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const connectionTagsQueryResult = useGetConnectionTagsQuery(currentWorkspaceId!);

    const {setIsWorkflowLoaded, setWorkflow, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setIsWorkflowLoaded: state.setIsWorkflowLoaded,
            setWorkflow: state.setWorkflow,
            workflow: state.workflow,
        }))
    );

    const queryClient = useQueryClient();

    const {data: currentWorkflow, isLoading: isWorkflowLoading} = useGetProjectWorkflowQuery(
        projectId,
        projectWorkflowId,
        projectId > 0 && projectWorkflowId > 0
    );

    const embeddedUseGetComponentDefinitionsQuery = (request: GetComponentDefinitionsRequestI, enabled?: boolean) =>
        // eslint-disable-next-line react-hooks/rules-of-hooks
        useGetComponentDefinitionsQuery(request, enabled);

    const embeddedUseGetConnectionsQuery = (request: RequestI, enabled?: boolean) =>
        // eslint-disable-next-line react-hooks/rules-of-hooks
        useGetWorkspaceConnectionsQuery(
            {
                environmentId: currentEnvironmentId,
                id: currentWorkspaceId!,
                ...request,
            },
            enabled
        );

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation();
    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation();
    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation();

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: (_result, variables) => {
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

    const cancelWorkflowQueries = () =>
        queryClient.cancelQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});

    const invalidateWorkflowQueries = () =>
        queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});

    const {runDisabled} = useRun();

    const [canvasWidth, setCanvasWidth] = useState(DEFAULT_CANVAS_WIDTH);

    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);
    const canvasContainerRef = useRef<HTMLDivElement>(null);

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const handleTestOutputCloseClick = () => bottomResizablePanelRef.current?.resize(0);

    const chatTrigger =
        workflow.triggers != null && workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1;

    useEffect(() => {
        setIsWorkflowLoaded(false);
    }, [projectWorkflowId, setIsWorkflowLoaded]);

    // The node-details, data-pill and test-chat panels are backed by module-level singleton stores shared with the
    // full Project.tsx editor (which resets them in useProject on projectWorkflowId change). Without the same reset
    // here, panels left open in a previous editor (e.g. a Project.tsx flow) stay open when a workflow is opened in the
    // AI Hub. Reset them whenever the embedded workflow changes so each workflow starts with a clean panel layout.
    useEffect(() => {
        useDataPillPanelStore.getState().setDataPillPanelOpen(false);
        useWorkflowTestChatStore.getState().setWorkflowTestChatPanelOpen(false);

        useWorkflowNodeDetailsPanelStore.getState().reset();
    }, [projectWorkflowId]);

    // The canvas layout centers nodes within `canvasWidth`. Left unset, the editor falls back to a
    // window-width-based estimate, which is far too wide inside the AI Hub's right panel — nodes end up
    // pushed off the right edge. Measuring the actual panel width keeps the flow centered, and the
    // ResizeObserver re-centers it when the user drags the chat/resource panel split.
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
                setWorkflow({...currentWorkflow});
            }, 0);

            return () => clearTimeout(timeoutId);
        }
    }, [currentWorkflow, isWorkflowLoading, setWorkflow]);

    return (
        <>
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            <WorkflowEditorProvider
                value={{
                    ConnectionKeys: ConnectionKeys,
                    cancelWorkflowQueries,
                    connectionTagsQueryKey: ConnectionKeys.connectionTags(currentWorkspaceId!),
                    deleteClusterElementParameterMutation,
                    deleteWorkflowNodeParameterMutation,
                    invalidateWorkflowQueries,
                    updateClusterElementParameterMutation,
                    updateWorkflowMutation: updateWorkflowEditorMutation,
                    updateWorkflowNodeParameterMutation,
                    useCreateConnectionMutation: useCreateConnectionMutation,
                    useGetComponentDefinitionsQuery: embeddedUseGetComponentDefinitionsQuery,
                    useGetConnectionTagsQuery: () => connectionTagsQueryResult,
                    useGetConnectionsQuery: embeddedUseGetConnectionsQuery,
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
                                    leftSidebarOpen={false}
                                    runDisabled={runDisabled}
                                    showCopilot={false}
                                    showWorkflowInputs={true}
                                    workflowReferenceId={projectWorkflowId}
                                />
                            </div>
                        </ResizablePanel>

                        <ResizableHandle className="bg-muted" />

                        <ResizablePanel className="flex" defaultSize={0} panelRef={bottomResizablePanelRef}>
                            {workflowTestExecution && (
                                <div className="m-3 flex flex-1 overflow-hidden rounded-lg bg-background">
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
 * <p>STATE-ISOLATION CAVEAT</p>
 * <p>The wrapping {@link WorkflowDataStoreProvider} creates a fresh per-instance store, but
 * {@link EmbeddableWorkflowEditorInner} (and every utility hook it transitively uses) currently reads from the
 * <em>module-level singleton</em> {@code useWorkflowDataStore}. As a result, mounting two embeddable editors at the
 * same time will share the singleton's state and the second mount will clobber the first. The AI Hub's
 * resource panel only ever shows a single workflow tab at a time so this is not user-visible today, but the
 * provider does NOT enforce isolation. Migrating utility hooks to {@code useWorkflowDataStoreContext} is the
 * follow-up needed to make this comment accurate.</p>
 *
 * Differences from the full Project.tsx editor:
 * - No project left sidebar (the project/workflow tree)
 * - No Copilot panel (the AI Hub chat IS the copilot)
 */
const EmbeddableWorkflowEditor = ({
    onWorkflowChange,
    projectId,
    projectWorkflowId,
    showPublishDeploy,
    showWorkflowSelect,
}: EmbeddableWorkflowEditorPropsI) => (
    <WorkflowDataStoreProvider>
        <EmbeddableWorkflowEditorInner
            onWorkflowChange={onWorkflowChange}
            projectId={projectId}
            projectWorkflowId={projectWorkflowId}
            showPublishDeploy={showPublishDeploy}
            showWorkflowSelect={showWorkflowSelect}
        />
    </WorkflowDataStoreProvider>
);

export default EmbeddableWorkflowEditor;
