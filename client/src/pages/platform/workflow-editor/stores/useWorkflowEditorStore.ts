/* eslint-disable sort-keys */
import {WorkflowTestExecutionModel} from '@/shared/middleware/platform/workflow/test';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowEditorI {
    showBottomPanel: boolean;
    setShowBottomPanelOpen: (showBottomPanel: boolean) => void;

    showPropertyCodeEditorSheet: boolean;
    setShowPropertyCodeEditorSheet: (showPropertyCodeEditorSheet: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    showWorkflowCodeEditorSheet: boolean;
    setShowWorkflowCodeEditorSheet: (showWorkflowCodeEditorSheet: boolean) => void;

    workflowTestExecution?: WorkflowTestExecutionModel;
    setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecutionModel) => void;

    workflowIsRunning: boolean;
    setWorkflowIsRunning: (workflowIsRunning: boolean) => void;
}

const useWorkflowEditorStore = create<WorkflowEditorI>()(
    devtools(
        (set) => ({
            showBottomPanel: false,
            setShowBottomPanelOpen: (showBottomPanel) =>
                set(() => ({
                    showBottomPanel,
                })),

            showPropertyCodeEditorSheet: false,
            setShowPropertyCodeEditorSheet: (showPropertyCodeEditorSheet) =>
                set(() => ({
                    showPropertyCodeEditorSheet,
                })),

            showEditWorkflowDialog: false,
            setShowEditWorkflowDialog: (showEditWorkflowDialog) =>
                set(() => ({
                    showEditWorkflowDialog: showEditWorkflowDialog,
                })),

            showWorkflowCodeEditorSheet: false,
            setShowWorkflowCodeEditorSheet: (showWorkflowCodeEditorSheet) =>
                set(() => ({
                    showWorkflowCodeEditorSheet,
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
            name: 'workflow-editor',
        }
    )
);

export default useWorkflowEditorStore;
