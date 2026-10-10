import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ClusterElementsCanvasDialogStateI {
    aiAgentSimpleEditorPreferred: boolean;
    copilotPanelOpen: boolean;
    dataStreamSimpleEditorPreferred: boolean;
    showAiAgentEditor: boolean;
    showDataStreamEditor: boolean;
    testingPanelOpen: boolean;
}

interface ClusterElementsCanvasDialogActionsI {
    reset: () => void;
    setAiAgentSimpleEditorPreferred: (preferred: boolean) => void;
    setCopilotPanelOpen: (open: boolean) => void;
    setDataStreamSimpleEditorPreferred: (preferred: boolean) => void;
    setShowAiAgentEditor: (show: boolean) => void;
    setShowDataStreamEditor: (show: boolean) => void;
    setTestingPanelOpen: (open: boolean) => void;
}

type ClusterElementsCanvasDialogStoreType = ClusterElementsCanvasDialogActionsI & ClusterElementsCanvasDialogStateI;

const initialState: ClusterElementsCanvasDialogStateI = {
    aiAgentSimpleEditorPreferred: true,
    copilotPanelOpen: false,
    dataStreamSimpleEditorPreferred: true,
    showAiAgentEditor: false,
    showDataStreamEditor: false,
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
                        aiAgentSimpleEditorPreferred: get().aiAgentSimpleEditorPreferred,
                        dataStreamSimpleEditorPreferred: get().dataStreamSimpleEditorPreferred,
                    })),

                setAiAgentSimpleEditorPreferred: (preferred) =>
                    set(() => ({
                        aiAgentSimpleEditorPreferred: preferred,
                    })),

                setCopilotPanelOpen: (open) =>
                    set(() => ({
                        copilotPanelOpen: open,
                    })),

                setDataStreamSimpleEditorPreferred: (preferred) =>
                    set(() => ({
                        dataStreamSimpleEditorPreferred: preferred,
                    })),

                setShowAiAgentEditor: (show) =>
                    set(() => ({
                        showAiAgentEditor: show,
                    })),

                setShowDataStreamEditor: (show) =>
                    set(() => ({
                        showDataStreamEditor: show,
                    })),

                setTestingPanelOpen: (open) =>
                    set(() => ({
                        testingPanelOpen: open,
                    })),
            }),
            {
                name: 'bytechef.cluster-elements-canvas-dialog-store',
                partialize: (state) => ({
                    aiAgentSimpleEditorPreferred: state.aiAgentSimpleEditorPreferred,
                    dataStreamSimpleEditorPreferred: state.dataStreamSimpleEditorPreferred,
                }),
            }
        )
    )
);
