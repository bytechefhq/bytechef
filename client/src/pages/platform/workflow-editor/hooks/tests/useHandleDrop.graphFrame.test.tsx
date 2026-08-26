import {ClickedDefinitionType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import useHandleDrop from '../useHandleDrop';

const {captureComponentUsedMock, fetchQueryMock, saveWorkflowDefinitionMock, updateWorkflowMutationMock} = vi.hoisted(
    () => ({
        captureComponentUsedMock: vi.fn(),
        fetchQueryMock: vi.fn(),
        saveWorkflowDefinitionMock: vi.fn(),
        updateWorkflowMutationMock: {isPending: false, mutate: vi.fn()},
    })
);

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: updateWorkflowMutationMock}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureComponentUsed: captureComponentUsedMock}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({fetchQuery: fetchQueryMock}),
}));

vi.mock('../../utils/saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

const GRAPH_ID = 'graph_1';
const PLACEHOLDER_ID = `${GRAPH_ID}-graph-placeholder`;

const WORKFLOW_DEFINITION = JSON.stringify({
    tasks: [{name: GRAPH_ID, parameters: {nodes: [], transitions: []}, type: 'graph/v1'}],
});

/** A component with no version, so `createWorkflowNodeData` needs no definition fetch. */
const DROPPED_COMPONENT = {name: 'httpClient', title: 'HTTP Client'} as ClickedDefinitionType;

/** A versioned component, so `createWorkflowNodeData` has to fetch its definition first. */
const DROPPED_VERSIONED_COMPONENT = {name: 'httpClient', title: 'HTTP Client', version: 1} as ClickedDefinitionType;

function buildPlaceholderNode(): Node {
    return {
        data: {graphId: GRAPH_ID, label: '+', taskDispatcherId: GRAPH_ID},
        hidden: true,
        id: PLACEHOLDER_ID,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };
}

function renderHandleDrop() {
    return renderHook(() => useHandleDrop({taskDispatcherDefinitions: []}));
}

function getGraphFrameDropHandler(result: {current: ReturnType<typeof useHandleDrop>}) {
    return result.current[4];
}

describe('useHandleDrop — dropping a component into a graph frame', () => {
    beforeEach(() => {
        saveWorkflowDefinitionMock.mockReset();
        saveWorkflowDefinitionMock.mockResolvedValue(undefined);

        captureComponentUsedMock.mockClear();

        fetchQueryMock.mockReset();

        useWorkflowEditorStore.getState().setGraphPendingConnection(undefined);
        useWorkflowDataStore.setState({
            nodes: [buildPlaceholderNode()],
            workflow: {definition: WORKFLOW_DEFINITION, id: 'workflow-1', nodeNames: []},
        });
    });

    // The pending connection is how the drop position reaches the insertion; it must be in place
    // BEFORE the insertion runs, and gone once the drop is over.
    it('carries the drop position through the graph add-node placeholder', async () => {
        const {result} = renderHandleDrop();

        saveWorkflowDefinitionMock.mockImplementation(() => {
            expect(useWorkflowEditorStore.getState().graphPendingConnection).toEqual({
                dropPosition: {x: 120, y: 40},
                from: '',
                graphId: GRAPH_ID,
            });

            return Promise.resolve();
        });

        await act(async () => {
            await getGraphFrameDropHandler(result)(GRAPH_ID, {x: 120, y: 40}, DROPPED_COMPONENT);
        });

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);

        const [saveArguments] = saveWorkflowDefinitionMock.mock.calls[0];

        expect(saveArguments.placeholderId).toBe(PLACEHOLDER_ID);
        expect(saveArguments.taskDispatcherContext).toMatchObject({graphId: GRAPH_ID, taskDispatcherId: GRAPH_ID});
    });

    // A drop already knows its component, so the picker must not open on top of the node it added.
    // The pending connection's empty `from` is the flag the frame reads; leaving it set after the
    // drop would also hand the NEXT add into that graph an abandoned drop position.
    it('leaves no pending connection behind once the drop is done', async () => {
        const {result} = renderHandleDrop();

        await act(async () => {
            await getGraphFrameDropHandler(result)(GRAPH_ID, {x: 120, y: 40}, DROPPED_COMPONENT);
        });

        expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
    });

    // A failed component-definition fetch aborts the drop before anything is inserted, so nothing
    // downstream consumes the pending connection — leaving it set would hand the abandoned drop
    // position to whatever is added into that graph next.
    it('leaves no pending connection behind when the drop fails', async () => {
        fetchQueryMock.mockRejectedValue(new Error('definition fetch failed'));

        const {result} = renderHandleDrop();

        await act(async () => {
            await expect(
                getGraphFrameDropHandler(result)(GRAPH_ID, {x: 120, y: 40}, DROPPED_VERSIONED_COMPONENT)
            ).rejects.toThrow('definition fetch failed');
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
    });

    it('does nothing when the graph has no add-node placeholder to insert through', async () => {
        useWorkflowDataStore.setState({nodes: []});

        const {result} = renderHandleDrop();

        await act(async () => {
            await getGraphFrameDropHandler(result)(GRAPH_ID, {x: 120, y: 40}, DROPPED_COMPONENT);
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
    });
});
