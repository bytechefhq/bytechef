import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ClusterElementsCanvasDialogStateI {
    copilotPanelOpen: boolean;
    editorPreferences: Record<string, boolean>;
    showAiAgentEditor: boolean;
    testingPanelOpen: boolean;
}

interface ClusterElementsCanvasDialogActionsI {
    reset: () => void;
    setCopilotPanelOpen: (open: boolean) => void;
    setEditorPreference: (agentNodeName: string, showAiAgent: boolean) => void;
    setShowAiAgentEditor: (show: boolean) => void;
    setTestingPanelOpen: (open: boolean) => void;
}

type ClusterElementsCanvasDialogStoreType = ClusterElementsCanvasDialogActionsI & ClusterElementsCanvasDialogStateI;

const initialState: ClusterElementsCanvasDialogStateI = {
    copilotPanelOpen: false,
    editorPreferences: {},
    showAiAgentEditor: false,
    testingPanelOpen: false,
};

export const useClusterElementsCanvasDialogStore = create<ClusterElementsCanvasDialogStoreType>()(
    devtools(
        persist(
            (set, get) => ({
                ...initialState,

                reset: () =>
                    set(() => ({
                        ...initialState,
                        editorPreferences: get().editorPreferences,
                    })),

                setCopilotPanelOpen: (open) =>
                    set(() => ({
                        copilotPanelOpen: open,
                    })),

                setEditorPreference: (agentNodeName, showAiAgent) =>
                    set(() => ({
                        editorPreferences: {...get().editorPreferences, [agentNodeName]: showAiAgent},
                    })),

                setShowAiAgentEditor: (show) =>
                    set(() => ({
                        showAiAgentEditor: show,
                    })),

                setTestingPanelOpen: (open) =>
                    set(() => ({
                        testingPanelOpen: open,
                    })),
            }),
            {
                name: 'bytechef.cluster-elements-canvas-dialog-store',
                partialize: (state) => ({editorPreferences: state.editorPreferences}),
            }
        )
    )
);
