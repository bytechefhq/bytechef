import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useWorkflowDataStore, {WorkflowDataType} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsCanvasDialog from '../useClusterElementsCanvasDialog';

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

const originalContext = {mode: MODE.BUILD, parameters: {workflowId: 'w1'}, source: Source.WORKFLOW_EDITOR};

const dataStreamDefinition = (processorType: string) =>
    JSON.stringify({
        tasks: [
            {
                clusterElements: {processor: {name: 'processor_1', type: processorType}},
                name: 'dataStream_1',
                type: 'dataStream/v1/sync',
            },
        ],
    });

const setRootClusterElement = (componentName: string, workflowNodeName: string) =>
    useWorkflowEditorStore.setState({
        rootClusterElementNodeData: {componentName, name: workflowNodeName, workflowNodeName} as NodeDataType,
    });

const setWorkflowDefinition = (definition: string) =>
    useWorkflowDataStore.setState({workflow: {definition, id: 'workflow-1'} as Workflow & WorkflowDataType});

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

        useAiAgentEvalsStore.setState({evalsPanelOpen: false});
        useClusterElementsCanvasDialogStore.setState({
            aiAgentSimpleEditorPreferred: true,
            copilotPanelOpen: false,
            dataStreamSimpleEditorPreferred: true,
            showAiAgentEditor: false,
            showDataStreamEditor: false,
            testingPanelOpen: false,
        });
        useCopilotStore.setState({
            context: originalContext,
            conversationStack: [],
            messages: [{content: 'editor conversation', role: 'user'}],
        });

        setRootClusterElement('aiAgent', 'aiAgent_1');
        setWorkflowDefinition(dataStreamDefinition('dataStreamProcessor/v1/script'));
    });

    describe('isDataStreamSimpleModeAvailable', () => {
        it('is available for a cluster root that is not a data stream', () => {
            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: vi.fn()}));

            expect(result.current.isDataStreamClusterRoot).toBe(false);
            expect(result.current.isDataStreamSimpleModeAvailable).toBe(true);
        });

        it('is unavailable for a data stream whose processor is not a field mapper', () => {
            setRootClusterElement('dataStream', 'dataStream_1');

            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: vi.fn()}));

            expect(result.current.isDataStreamSimpleModeAvailable).toBe(false);
            expect(useClusterElementsCanvasDialogStore.getState().showDataStreamEditor).toBe(false);
        });

        it('is available for a data stream whose processor is a field mapper', () => {
            setRootClusterElement('dataStream', 'dataStream_1');
            setWorkflowDefinition(dataStreamDefinition('dataStreamProcessor/v1/fieldMapper'));

            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: vi.fn()}));

            expect(result.current.isDataStreamSimpleModeAvailable).toBe(true);
            expect(useClusterElementsCanvasDialogStore.getState().showDataStreamEditor).toBe(true);
        });
    });

    describe('copilot conversation', () => {
        it('saves the current conversation and starts a cluster element one when the copilot opens', () => {
            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: vi.fn()}));

            act(() => result.current.handleCopilotClick());

            const state = useCopilotStore.getState();

            expect(state.conversationStack).toHaveLength(1);
            expect(state.messages).toEqual([]);
            expect(state.context).toEqual({
                mode: MODE.ASK,
                parameters: {taskName: 'aiAgent_1'},
                source: Source.CLUSTER_ELEMENT,
            });
            expect(useClusterElementsCanvasDialogStore.getState().copilotPanelOpen).toBe(true);
        });

        it('restores the saved conversation when the copilot closes', () => {
            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: vi.fn()}));

            act(() => result.current.handleCopilotClick());

            act(() => result.current.handleCopilotClose());

            const state = useCopilotStore.getState();

            expect(state.conversationStack).toHaveLength(0);
            expect(state.context).toEqual(originalContext);
            expect(state.messages).toEqual([{content: 'editor conversation', role: 'user'}]);
            expect(useClusterElementsCanvasDialogStore.getState().copilotPanelOpen).toBe(false);
        });

        it('restores the saved conversation and closes the evals panel when the dialog closes', () => {
            const onOpenChange = vi.fn();

            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange}));

            act(() => result.current.handleCopilotClick());

            useAiAgentEvalsStore.setState({evalsPanelOpen: true});

            act(() => result.current.handleClose());

            expect(onOpenChange).toHaveBeenCalledWith(false);
            expect(useCopilotStore.getState().context).toEqual(originalContext);
            expect(useCopilotStore.getState().conversationStack).toHaveLength(0);
            expect(useAiAgentEvalsStore.getState().evalsPanelOpen).toBe(false);
        });

        it('keeps the conversation and the evals panel when the dialog opens', () => {
            const onOpenChange = vi.fn();

            const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange}));

            act(() => result.current.handleCopilotClick());

            useAiAgentEvalsStore.setState({evalsPanelOpen: true});

            act(() => result.current.handleOpenChange(true));

            expect(onOpenChange).toHaveBeenCalledWith(true);
            expect(useCopilotStore.getState().conversationStack).toHaveLength(1);
            expect(useAiAgentEvalsStore.getState().evalsPanelOpen).toBe(true);
        });
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
