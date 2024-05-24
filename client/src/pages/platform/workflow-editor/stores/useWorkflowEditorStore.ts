/* eslint-disable sort-keys */
import {WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowEditorI {
    showBottomPanelOpen: boolean;
    setShowBottomPanelOpen: (showBottomPanelOpen: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    workflowCodeEditorSheetOpen: boolean;
    setWorkflowCodeEditorSheetOpen: (workflowCodeEditorSheetOpen: boolean) => void;

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

            workflowCodeEditorSheetOpen: false,
            setWorkflowCodeEditorSheetOpen: (workflowCodeEditorSheetOpen) =>
                set(() => ({
                    workflowCodeEditorSheetOpen: workflowCodeEditorSheetOpen,
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
