/* eslint-disable sort-keys */
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementCallRecord, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {NestedClusterRootComponentDefinitionType, NodeDataType} from '@/shared/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface PendingClusterElementDropI {
    componentDefinition: ComponentDefinition;
    placeholderId: string;
}

export interface WorkflowTestNodeStateI {
    clusterElement?: boolean;
    durationMillis?: number;
    error?: string;
    status: 'RUNNING' | 'COMPLETED' | 'FAILED';
}

export interface WorkflowEditorI {
    clusterElementsCanvasOpen: boolean;
    setClusterElementsCanvasOpen: (clusterElementsCanvasOpen: boolean) => void;

    clusterFrameLockedByRootId: Record<string, boolean>;
    setClusterFrameLocked: (clusterRootId: string, locked: boolean) => void;

    copiedNode: NodeDataType | undefined;
    setCopiedNode: (copiedNode: NodeDataType | undefined) => void;

    copiedWorkflowId: string | undefined;
    setCopiedWorkflowId: (copiedWorkflowId: string | undefined) => void;

    clusterRootComponentDefinitions: Record<string, ComponentDefinition>;
    setClusterRootComponentDefinition: (clusterRootId: string, definition: ComponentDefinition) => void;

    nodesLocked: boolean;
    setNodesLocked: (nodesLocked: boolean) => void;

    pendingClusterElementDrop: PendingClusterElementDropI | undefined;
    setPendingClusterElementDrop: (pendingClusterElementDrop: PendingClusterElementDropI | undefined) => void;

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

    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>;
    setWorkflowTestNodeState: (nodeName: string, nodeState: WorkflowTestNodeStateI) => void;
    removeWorkflowTestNodeState: (nodeName: string) => void;
    resetWorkflowTestNodeStates: (workflowId: string | undefined) => void;
    workflowTestNodeStatesWorkflowId: string | undefined;

    workflowTestClusterElementCallRecords: Record<string, ClusterElementCallRecord[]>;
    upsertWorkflowTestClusterElementCallRecord: (taskExecutionId: string, record: ClusterElementCallRecord) => void;
}

const useWorkflowEditorStore = create<WorkflowEditorI>()(
    devtools(
        (set) => ({
            clusterElementsCanvasOpen: false,
            setClusterElementsCanvasOpen: (clusterElementsCanvasOpen) =>
                set(() => ({
                    clusterElementsCanvasOpen,
                })),

            clusterFrameLockedByRootId: {},
            setClusterFrameLocked: (clusterRootId, locked) =>
                set((state) => ({
                    clusterFrameLockedByRootId: {
                        ...state.clusterFrameLockedByRootId,
                        [clusterRootId]: locked,
                    },
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

            clusterRootComponentDefinitions: {},

            setClusterRootComponentDefinition: (clusterRootId, definition) =>
                set((state) => ({
                    clusterRootComponentDefinitions: {
                        ...state.clusterRootComponentDefinitions,
                        [clusterRootId]: definition,
                    },
                })),

            nodesLocked: true,
            setNodesLocked: (nodesLocked) =>
                set(() => ({
                    nodesLocked,
                })),

            pendingClusterElementDrop: undefined,
            setPendingClusterElementDrop: (pendingClusterElementDrop) =>
                set(() => ({
                    pendingClusterElementDrop,
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
            setWorkflowTestNodeState: (nodeName, nodeState) =>
                set((state) => ({
                    workflowTestNodeStates: {...state.workflowTestNodeStates, [nodeName]: nodeState},
                })),
            removeWorkflowTestNodeState: (nodeName) =>
                set((state) => {
                    const workflowTestNodeStates = {...state.workflowTestNodeStates};

                    delete workflowTestNodeStates[nodeName];

                    return {workflowTestNodeStates};
                }),
            resetWorkflowTestNodeStates: (workflowId) =>
                set(() => ({
                    workflowTestClusterElementCallRecords: {},
                    workflowTestNodeStates: {},
                    workflowTestNodeStatesWorkflowId: workflowId,
                })),

            workflowTestNodeStatesWorkflowId: undefined,

            workflowTestClusterElementCallRecords: {},
            upsertWorkflowTestClusterElementCallRecord: (taskExecutionId, record) =>
                set((state) => {
                    const records = state.workflowTestClusterElementCallRecords[taskExecutionId] ?? [];
                    const existingIndex = records.findIndex((existing) => existing.id === record.id);

                    if (existingIndex === -1) {
                        return {
                            workflowTestClusterElementCallRecords: {
                                ...state.workflowTestClusterElementCallRecords,
                                [taskExecutionId]: [...records, record],
                            },
                        };
                    }

                    const definedFields = Object.fromEntries(
                        Object.entries(record).filter(([, value]) => value !== undefined)
                    );
                    const nextRecords = [...records];

                    nextRecords[existingIndex] = {...records[existingIndex], ...definedFields};

                    return {
                        workflowTestClusterElementCallRecords: {
                            ...state.workflowTestClusterElementCallRecords,
                            [taskExecutionId]: nextRecords,
                        },
                    };
                }),
        }),
        {
            name: 'workflow-editor',
        }
    )
);

export default useWorkflowEditorStore;
