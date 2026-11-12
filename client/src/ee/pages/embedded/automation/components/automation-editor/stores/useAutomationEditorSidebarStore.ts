import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AutomationEditorSidebarStateI {
    leftSidebarOpen: boolean;
    setLeftSidebarOpen: (leftSidebarOpen: boolean) => void;
}

export const useAutomationEditorSidebarStore = create<AutomationEditorSidebarStateI>()(
    devtools(
        persist(
            (set) => ({
                leftSidebarOpen: false,
                setLeftSidebarOpen: (leftSidebarOpen) => set(() => ({leftSidebarOpen})),
            }),
            {name: 'bytechef.automation-editor-sidebar'}
        )
    )
);

export default useAutomationEditorSidebarStore;
