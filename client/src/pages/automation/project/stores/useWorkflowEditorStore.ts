/* eslint-disable sort-keys */
import {WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowEditorI {
    showBottomPanelOpen: boolean;
    setShowBottomPanelOpen: (showBottomPanelOpen: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    showProjectVersionHistorySheet: boolean;
    setShowProjectVersionHistorySheet: (showProjectVersionHistorySheet: boolean) => void;

    showWorkflowCodeEditorSheet: boolean;
    setShowWorkflowCodeEditorSheet: (showWorkflowCodeEditorSheet: boolean) => void;

    showWorkflowInputsSheet: boolean;
    setShowWorkflowInputsSheet: (showWorkflowInputsSheet: boolean) => void;

    showWorkflowOutputsSheet: boolean;
    setShowWorkflowOutputsSheet: (showWorkflowOutputsSheet: boolean) => void;

    workflowTestExecution?: WorkflowTestExecutionModel;
    setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecutionModel) => void;

    workflowIsRunning: boolean;
    setWorkflowIsRunning: (workflowIsRunning: boolean) => void;
}

const useWorkflowEditorStore = create<WorkflowEditorI>()(
    devtools(
        (set) => ({
            showBottomPanelOpen: false,
            setShowBottomPanelOpen: (showBottomPanelOpen) =>
                set(() => ({
                    showBottomPanelOpen: showBottomPanelOpen,
                })),

            showEditWorkflowDialog: false,
            setShowEditWorkflowDialog: (showEditWorkflowDialog) =>
                set(() => ({
                    showEditWorkflowDialog: showEditWorkflowDialog,
                })),

            showProjectVersionHistorySheet: false,
            setShowProjectVersionHistorySheet: (showProjectVersionHistorySheet) =>
                set(() => ({
                    showProjectVersionHistorySheet: showProjectVersionHistorySheet,
                })),

            showWorkflowCodeEditorSheet: false,
            setShowWorkflowCodeEditorSheet: (showWorkflowCodeEditorSheet) =>
                set(() => ({
                    showWorkflowCodeEditorSheet: showWorkflowCodeEditorSheet,
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

            workflowTestExecution: undefined,
            setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecutionModel) =>
                set(() => ({
                    workflowTestExecution: workflowTestExecution,
                })),

            workflowIsRunning: false,
            setWorkflowIsRunning: (workflowIsRunning) =>
                set(() => ({
                    workflowIsRunning: workflowIsRunning,
                })),
        }),
        {
            name: 'project',
        }
    )
);

export default useWorkflowEditorStore;
