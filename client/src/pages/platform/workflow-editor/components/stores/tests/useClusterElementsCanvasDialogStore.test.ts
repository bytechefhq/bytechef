import {beforeEach, describe, expect, it} from 'vitest';

import {useClusterElementsCanvasDialogStore} from '../useClusterElementsCanvasDialogStore';

describe('useClusterElementsCanvasDialogStore', () => {
    beforeEach(() => {
        localStorage.clear();

        useClusterElementsCanvasDialogStore.setState({
            aiAgentSimpleEditorPreferred: true,
            copilotPanelOpen: false,
            dataStreamSimpleEditorPreferred: true,
            showAiAgentEditor: false,
            showDataStreamEditor: false,
            testingPanelOpen: false,
        });
    });

    it('prefers the simple editors by default', () => {
        const state = useClusterElementsCanvasDialogStore.getState();

        expect(state.aiAgentSimpleEditorPreferred).toBe(true);
        expect(state.dataStreamSimpleEditorPreferred).toBe(true);
    });

    it('records the AI Agent and DataStream preferences independently', () => {
        useClusterElementsCanvasDialogStore.getState().setAiAgentSimpleEditorPreferred(false);

        expect(useClusterElementsCanvasDialogStore.getState().aiAgentSimpleEditorPreferred).toBe(false);
        expect(useClusterElementsCanvasDialogStore.getState().dataStreamSimpleEditorPreferred).toBe(true);

        useClusterElementsCanvasDialogStore.getState().setDataStreamSimpleEditorPreferred(false);

        expect(useClusterElementsCanvasDialogStore.getState().dataStreamSimpleEditorPreferred).toBe(false);
    });

    it('keeps the editor preferences but clears the panel state on reset', () => {
        const store = useClusterElementsCanvasDialogStore.getState();

        store.setAiAgentSimpleEditorPreferred(false);
        store.setDataStreamSimpleEditorPreferred(false);
        store.setCopilotPanelOpen(true);
        store.setShowAiAgentEditor(true);
        store.setShowDataStreamEditor(true);
        store.setTestingPanelOpen(true);

        useClusterElementsCanvasDialogStore.getState().reset();

        const state = useClusterElementsCanvasDialogStore.getState();

        expect(state.aiAgentSimpleEditorPreferred).toBe(false);
        expect(state.dataStreamSimpleEditorPreferred).toBe(false);
        expect(state.copilotPanelOpen).toBe(false);
        expect(state.showAiAgentEditor).toBe(false);
        expect(state.showDataStreamEditor).toBe(false);
        expect(state.testingPanelOpen).toBe(false);
    });

    it('persists only the editor preferences', () => {
        useClusterElementsCanvasDialogStore.getState().setCopilotPanelOpen(true);
        useClusterElementsCanvasDialogStore.getState().setAiAgentSimpleEditorPreferred(false);

        const persisted = JSON.parse(localStorage.getItem('bytechef.cluster-elements-canvas-dialog-store') ?? '{}');

        expect(persisted.state).toEqual({
            aiAgentSimpleEditorPreferred: false,
            dataStreamSimpleEditorPreferred: true,
        });
    });
});
