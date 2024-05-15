/* eslint-disable sort-keys */
import {WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowEditorI {
    showBottomPanelOpen: boolean;
    setShowBottomPanelOpen: (showBottomPanelOpen: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    workflowTestConfigurationDialogOpen: boolean;
    setWorkflowTestConfigurationDialogOpen: (workflowTestConfigurationDialogOpen: boolean) => void;

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

            workflowTestConfigurationDialogOpen: false,
            setWorkflowTestConfigurationDialogOpen: (workflowTestConfigurationDialogOpen) =>
                set(() => ({
                    workflowTestConfigurationDialogOpen: workflowTestConfigurationDialogOpen,
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
