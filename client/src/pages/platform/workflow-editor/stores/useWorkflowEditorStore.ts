/* eslint-disable sort-keys */
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {NestedClusterRootComponentDefinitionType, NodeDataType} from '@/shared/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface WorkflowEditorI {
    clusterElementsCanvasOpen: boolean;
    setClusterElementsCanvasOpen: (clusterElementsCanvasOpen: boolean) => void;

    mainClusterRootComponentDefinition: ComponentDefinition | undefined;
    setMainClusterRootComponentDefinition: (
        mainClusterRootComponentDefinition: ComponentDefinition | undefined
    ) => void;

    nestedClusterRootsComponentDefinitions: Record<string, NestedClusterRootComponentDefinitionType>;
    setNestedClusterRootsComponentDefinitions: (
        setNestedClusterRootsComponentDefinitions: Record<string, NestedClusterRootComponentDefinitionType>
    ) => void;

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

    workflowTestExecution?: WorkflowTestExecution;
    setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecution) => void;
}

const useWorkflowEditorStore = create<WorkflowEditorI>()(
    devtools(
        (set) => ({
            clusterElementsCanvasOpen: false,
            setClusterElementsCanvasOpen: (clusterElementsCanvasOpen) =>
                set(() => ({
                    clusterElementsCanvasOpen,
                })),

            mainClusterRootComponentDefinition: undefined,
            setMainClusterRootComponentDefinition: (mainClusterRootComponentDefinition) =>
                set(() => ({
                    mainClusterRootComponentDefinition,
                })),

            nestedClusterRootsComponentDefinitions: {},
            setNestedClusterRootsComponentDefinitions: (nestedClusterRootsComponentDefinitions) =>
                set(() => ({
                    nestedClusterRootsComponentDefinitions,
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

            workflowTestExecution: undefined,
            setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecution) =>
                set(() => ({
                    workflowTestExecution: workflowTestExecution,
                })),
        }),
        {
            name: 'workflow-editor',
        }
    )
);

export default useWorkflowEditorStore;
