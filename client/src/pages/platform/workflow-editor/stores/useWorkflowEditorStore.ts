/* eslint-disable sort-keys */
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowEditorI {
    showBottomPanel: boolean;
    setShowBottomPanelOpen: (showBottomPanel: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    setShowPropertyCodeEditorSheet: (showPropertyCodeEditorSheet: boolean) => void;
    showWorkflowCodeEditorSheet: boolean;

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
