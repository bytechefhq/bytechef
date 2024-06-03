import {WorkflowTestExecutionModel} from '@/shared/middleware/platform/workflow/test';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowDataStateI {
    projectId: number;
    setProjectId: (projectId: number) => void;

    showBottomPanelOpen: boolean;
    setShowBottomPanelOpen: (showBottomPanelOpen: boolean) => void;

    showEditWorkflowDialog: boolean;
    setShowEditWorkflowDialog: (showEditWorkflowDialog: boolean) => void;

    workflowTestExecution?: WorkflowTestExecutionModel;
    setWorkflowTestExecution: (workflowTestExecution?: WorkflowTestExecutionModel) => void;

    workflowIsRunning: boolean;
    setWorkflowIsRunning: (workflowIsRunning: boolean) => void;
}

const useProjectDataStore = create<WorkflowDataStateI>()(
    devtools(
        (set) => ({
            projectId: 0,
            setProjectId: (projectId: number) => set((state) => ({...state, projectId})),

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
        {name: 'project-data'}
    )
);

export default useProjectDataStore;
