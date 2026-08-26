/* eslint-disable sort-keys */
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {GraphPendingConnectionType, NestedClusterRootComponentDefinitionType, NodeDataType} from '@/shared/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface WorkflowTestNodeStateI {
    durationMillis?: number;
    error?: string;
    status: 'RUNNING' | 'COMPLETED' | 'FAILED';
}

export interface WorkflowEditorI {
    clusterElementsCanvasOpen: boolean;
    setClusterElementsCanvasOpen: (clusterElementsCanvasOpen: boolean) => void;

    copiedNode: NodeDataType | undefined;
    setCopiedNode: (copiedNode: NodeDataType | undefined) => void;

    copiedWorkflowId: string | undefined;
    setCopiedWorkflowId: (copiedWorkflowId: string | undefined) => void;

    /**
     * A graph transition dropped on empty frame space, or a component dropped into a frame, waiting
     * for the popover to name the task it creates. Consumed (and cleared) by
     * `insertTaskDispatcherSubtask`, which is where that task is appended to `parameters.nodes`.
     */
    graphPendingConnection: GraphPendingConnectionType | undefined;
    setGraphPendingConnection: (graphPendingConnection: GraphPendingConnectionType | undefined) => void;

    /**
     * Where each graph transition edge's label sits, in flow coordinates, published by the edge as it
     * renders. It is the ONLY thing the canvas-level transition editor needs from the edge, and the
     * reason it is a store entry rather than a prop: React Flow recreates its edge components on
     * every relayout, so an editor rendered inside one lost its caret, its data pill popup and its
     * pending save each time the canvas settled. Read alongside the edge's own `selected` flag, which
     * stays the source of truth for whether an editor is open — a position left behind by a deleted
     * or deselected edge is simply never looked up.
     */
    graphTransitionLabelPositions: Record<string, {labelX: number; labelY: number}>;
    setGraphTransitionLabelPosition: (edgeId: string, position: {labelX: number; labelY: number}) => void;

    mainClusterRootComponentDefinition: ComponentDefinition | undefined;
    setMainClusterRootComponentDefinition: (
        mainClusterRootComponentDefinition: ComponentDefinition | undefined
    ) => void;

    nodesLocked: boolean;
    setNodesLocked: (nodesLocked: boolean) => void;

    nestedClusterRootsComponentDefinitions: Record<string, NestedClusterRootComponentDefinitionType>;
    setNestedClusterRootsComponentDefinitions: (
        setNestedClusterRootsComponentDefinitions: Record<string, NestedClusterRootComponentDefinitionType>
    ) => void;

    renamingNodeName: string | undefined;
    setRenamingNodeName: (renamingNodeName: string | undefined) => void;

    resetWorkflowLayout: boolean;
    setResetWorkflowLayout: (resetWorkflowLayout: boolean) => void;

    rootClusterElementNodeData: NodeDataType | undefined;
    setRootClusterElementNodeData: (rootClusterElementNodeData: NodeDataType | undefined) => void;

    showBottomPanel: boolean;
    setShowBottomPanelOpen: (showBottomPanel: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    showWorkflowCodeEditorSheet: boolean;
    setShowPropertyCodeEditorSheet: (showPropertyCodeEditorSheet: boolean) => void;

    setShowWorkflowCodeEditorSheet: (showWorkflowCodeEditorSheet: boolean) => void;
    workflowIsRunning: boolean;

    showPropertyCodeEditorSheet: boolean;
    setWorkflowIsRunning: (workflowIsRunning: boolean) => void;

    showWorkflowInputsSheet: boolean;
    setShowWorkflowInputsSheet: (showWorkflowInputsSheet: boolean) => void;

    showWorkflowOutputsSheet: boolean;
    setShowWorkflowOutputsSheet: (showWorkflowOutputsSheet: boolean) => void;

    parentWorkflowTestExecution?: WorkflowTestExecution;
    setParentWorkflowTestExecution: (parentWorkflowTestExecution?: WorkflowTestExecution) => void;

    workflowTestExecution?: WorkflowTestExecution;
    setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecution) => void;

    /**
     * Live per-node status of the last test run, keyed by node name. Node names (`var_1`, `task_1`, ...)
     * are only unique within a workflow, so the id of the workflow the run belongs to is kept alongside
     * them -- read them through `useWorkflowTestNodeStates`, never straight off the store, or another
     * workflow's nodes light up wherever the names happen to coincide.
     */
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>;
    workflowTestNodeStatesWorkflowId: string | undefined;
    setWorkflowTestNodeState: (nodeName: string, nodeState: WorkflowTestNodeStateI) => void;
    resetWorkflowTestNodeStates: (workflowId: string | undefined) => void;
}

const useWorkflowEditorStore = create<WorkflowEditorI>()(
    devtools(
        (set) => ({
            clusterElementsCanvasOpen: false,
            setClusterElementsCanvasOpen: (clusterElementsCanvasOpen) =>
                set(() => ({
                    clusterElementsCanvasOpen,
                })),

            copiedNode: undefined,
            setCopiedNode: (copiedNode) =>
                set(() => ({
                    copiedNode,
                })),

            copiedWorkflowId: undefined,
            setCopiedWorkflowId: (copiedWorkflowId) =>
                set(() => ({
                    copiedWorkflowId,
                })),

            graphPendingConnection: undefined,
            setGraphPendingConnection: (graphPendingConnection) =>
                set(() => ({
                    graphPendingConnection,
                })),

            graphTransitionLabelPositions: {},
            setGraphTransitionLabelPosition: (edgeId, position) =>
                set((state) => {
                    const currentPosition = state.graphTransitionLabelPositions[edgeId];

                    // Every edge republishes on every render, and the canvas renders on every tween
                    // frame — so writing an unchanged position would loop the whole canvas back
                    // through the store for the length of each animation.
                    if (currentPosition?.labelX === position.labelX && currentPosition?.labelY === position.labelY) {
                        return state;
                    }

                    return {
                        graphTransitionLabelPositions: {
                            ...state.graphTransitionLabelPositions,
                            [edgeId]: position,
                        },
                    };
                }),

            mainClusterRootComponentDefinition: undefined,
            setMainClusterRootComponentDefinition: (mainClusterRootComponentDefinition) =>
                set(() => ({
                    mainClusterRootComponentDefinition,
                })),

            nodesLocked: true,
            setNodesLocked: (nodesLocked) =>
                set(() => ({
                    nodesLocked,
                })),

            nestedClusterRootsComponentDefinitions: {},
            setNestedClusterRootsComponentDefinitions: (nestedClusterRootsComponentDefinitions) =>
                set(() => ({
                    nestedClusterRootsComponentDefinitions,
                })),

            renamingNodeName: undefined,
            setRenamingNodeName: (renamingNodeName) =>
                set(() => ({
                    renamingNodeName,
                })),

            resetWorkflowLayout: false,
            setResetWorkflowLayout: (resetWorkflowLayout) =>
                set(() => ({
                    resetWorkflowLayout,
                })),

            rootClusterElementNodeData: undefined,
            setRootClusterElementNodeData: (rootClusterElementNodeData) =>
                set(() => ({
                    rootClusterElementNodeData,
                })),

            showBottomPanel: false,
            setShowBottomPanelOpen: (showBottomPanel) =>
                set(() => ({
                    showBottomPanel,
                })),

            showEditWorkflowDialog: false,
            setShowEditWorkflowDialog: (showEditWorkflowDialog) =>
                set(() => ({
                    showEditWorkflowDialog: showEditWorkflowDialog,
                })),

            showPropertyCodeEditorSheet: false,
            setShowPropertyCodeEditorSheet: (showPropertyCodeEditorSheet) =>
                set(() => ({
                    showPropertyCodeEditorSheet,
                })),

            showWorkflowCodeEditorSheet: false,
            setShowWorkflowCodeEditorSheet: (showWorkflowCodeEditorSheet) =>
                set(() => ({
                    showWorkflowCodeEditorSheet,
                })),

            showWorkflowInputsSheet: false,
            setShowWorkflowInputsSheet: (showWorkflowInputsSheet) =>
                set(() => ({
                    showWorkflowInputsSheet: showWorkflowInputsSheet,
                })),

            showWorkflowOutputsSheet: false,
            setShowWorkflowOutputsSheet: (showWorkflowOutputsSheet) =>
                set(() => ({
                    showWorkflowOutputsSheet: showWorkflowOutputsSheet,
                })),

            workflowIsRunning: false,
            setWorkflowIsRunning: (workflowIsRunning) =>
                set(() => ({
                    workflowIsRunning: workflowIsRunning,
                })),

            parentWorkflowTestExecution: undefined,
            setParentWorkflowTestExecution: (parentWorkflowTestExecution?: WorkflowTestExecution) =>
                set(() => ({
                    parentWorkflowTestExecution,
                })),

            workflowTestExecution: undefined,
            setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecution) =>
                set(() => ({
                    workflowTestExecution: workflowTestExecution,
                })),

            workflowTestNodeStates: {},
            workflowTestNodeStatesWorkflowId: undefined,
            setWorkflowTestNodeState: (nodeName, nodeState) =>
                set((state) => ({
                    workflowTestNodeStates: {...state.workflowTestNodeStates, [nodeName]: nodeState},
                })),
            resetWorkflowTestNodeStates: (workflowId) =>
                set(() => ({
                    workflowTestNodeStates: {},
                    workflowTestNodeStatesWorkflowId: workflowId,
                })),
        }),
        {
            name: 'workflow-editor',
        }
    )
);

export default useWorkflowEditorStore;
