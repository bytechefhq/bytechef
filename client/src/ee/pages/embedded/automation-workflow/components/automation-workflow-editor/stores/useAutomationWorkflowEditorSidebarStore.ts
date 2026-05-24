import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AutomationWorkflowEditorSidebarStateI {
    leftSidebarOpen: boolean;
    setLeftSidebarOpen: (leftSidebarOpen: boolean) => void;
}

export const useAutomationWorkflowEditorSidebarStore = create<AutomationWorkflowEditorSidebarStateI>()(
    devtools(
        persist(
            (set) => ({
                leftSidebarOpen: true,
                setLeftSidebarOpen: (leftSidebarOpen) => set(() => ({leftSidebarOpen})),
            }),
            {name: 'bytechef.automation-workflow-editor-sidebar'}
        )
    )
);

export default useAutomationWorkflowEditorSidebarStore;
