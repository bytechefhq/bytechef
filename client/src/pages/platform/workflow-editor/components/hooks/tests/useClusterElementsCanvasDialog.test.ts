import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsCanvasDialog from '../useClusterElementsCanvasDialog';

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: false}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

function setRootClusterElement(componentName: string, workflowNodeName: string) {
    useWorkflowEditorStore.setState({
        rootClusterElementNodeData: {componentName, name: workflowNodeName, workflowNodeName} as NodeDataType,
    });
}

function setDataStreamProcessor(workflowNodeName: string, processorType: string) {
    const definition = JSON.stringify({
        tasks: [{clusterElements: {processor: {type: processorType}}, name: workflowNodeName, type: 'dataStream/v1'}],
    });

    useWorkflowDataStore.setState((state) => ({workflow: {...state.workflow, definition, id: 'workflow-1'}}));
}

function renderDialogHook() {
    return renderHook(() => useClusterElementsCanvasDialog({onOpenChange: vi.fn()}));
}

describe('useClusterElementsCanvasDialog', () => {
    beforeEach(() => {
        localStorage.clear();

        useClusterElementsCanvasDialogStore.setState({
            aiAgentSimpleEditorPreferred: true,
            dataStreamSimpleEditorPreferred: true,
            showAiAgentEditor: false,
            showDataStreamEditor: false,
            testingPanelOpen: false,
        });

        useWorkflowDataStore.setState((state) => ({workflow: {...state.workflow, definition: undefined}}));
    });

    describe('AI Agent', () => {
        it('opens the simple editor by default', () => {
            setRootClusterElement('aiAgent', 'aiAgent_1');

            renderDialogHook();

            expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(true);
        });

        it('applies a toggle on one agent to every other agent', () => {
            setRootClusterElement('aiAgent', 'aiAgent_1');

            const {result, unmount} = renderDialogHook();

            act(() => result.current.handleToggleEditor(false));

            expect(useClusterElementsCanvasDialogStore.getState().aiAgentSimpleEditorPreferred).toBe(false);
            expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(false);

            unmount();

            act(() => setRootClusterElement('aiAgent', 'aiAgent_2'));

            renderDialogHook();

            expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(false);
        });

        it('does not change the DataStream preference', () => {
            setRootClusterElement('aiAgent', 'aiAgent_1');

            const {result} = renderDialogHook();

            act(() => result.current.handleToggleEditor(false));

            expect(useClusterElementsCanvasDialogStore.getState().dataStreamSimpleEditorPreferred).toBe(true);
        });
    });

    describe('DataStream', () => {
        it('applies a toggle on one DataStream to every other DataStream', () => {
            setDataStreamProcessor('dataStream_1', 'dataStreamProcessor/v1/fieldMapper');
            setRootClusterElement('dataStream', 'dataStream_1');

            const {result, unmount} = renderDialogHook();

            expect(useClusterElementsCanvasDialogStore.getState().showDataStreamEditor).toBe(true);

            act(() => result.current.handleToggleEditor(false));

            expect(useClusterElementsCanvasDialogStore.getState().dataStreamSimpleEditorPreferred).toBe(false);
            expect(useClusterElementsCanvasDialogStore.getState().aiAgentSimpleEditorPreferred).toBe(true);

            unmount();

            act(() => {
                setDataStreamProcessor('dataStream_2', 'dataStreamProcessor/v1/fieldMapper');
                setRootClusterElement('dataStream', 'dataStream_2');
            });

            renderDialogHook();

            expect(useClusterElementsCanvasDialogStore.getState().showDataStreamEditor).toBe(false);
        });

        it('opens the advanced editor when the processor is not the field mapper', () => {
            setDataStreamProcessor('dataStream_1', 'dataStreamProcessor/v1/script');
            setRootClusterElement('dataStream', 'dataStream_1');

            const {result} = renderDialogHook();

            expect(result.current.isDataStreamSimpleModeAvailable).toBe(false);
            expect(useClusterElementsCanvasDialogStore.getState().showDataStreamEditor).toBe(false);
            expect(useClusterElementsCanvasDialogStore.getState().dataStreamSimpleEditorPreferred).toBe(true);
        });
    });

    it('shows neither simple editor for other cluster roots', () => {
        setRootClusterElement('dataMapper', 'dataMapper_1');

        renderDialogHook();

        expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(false);
        expect(useClusterElementsCanvasDialogStore.getState().showDataStreamEditor).toBe(false);
    });
});
