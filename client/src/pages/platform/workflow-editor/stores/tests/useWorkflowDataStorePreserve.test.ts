import {Workflow} from '@/shared/middleware/platform/configuration';
import {Edge, Node} from '@xyflow/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore, {preserveWorkflowDataStore} from '../useWorkflowDataStore';

const COALESCE_MS = 100;

const editorNodes = [{data: {}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'}] as Node[];
const editorEdges = [{id: 'trigger_1=>task_1', source: 'trigger_1', target: 'task_1'}] as Edge[];
const readOnlyNodes = [{data: {}, id: 'trigger_1', position: {x: 5, y: 5}, type: 'readonly'}] as Node[];

const editorWorkflow = {
    definition: JSON.stringify({tasks: [{name: 'task_1', type: 'httpClient/v1/get'}]}),
    id: 'wf-1',
    nodeNames: ['trigger_1', 'task_1'],
    tasks: [{name: 'task_1', type: 'httpClient/v1/get'}],
} as Workflow & {nodeNames: string[]};

function temporal() {
    return useWorkflowDataStore.temporal.getState();
}

function simulateReadOnlyCanvasUsingTheStore() {
    useWorkflowDataStore.getState().setNodes(readOnlyNodes);
    useWorkflowDataStore.getState().setEdges([]);
}

describe('preserveWorkflowDataStore', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        temporal().pause();

        useWorkflowDataStore.getState().reset();

        temporal().clear();
        temporal().resume();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('restores the loaded editor workflow and canvas when a read-only view is done with the store', () => {
        temporal().pause();

        useWorkflowDataStore.setState({
            edges: editorEdges,
            isWorkflowLoaded: true,
            nodes: editorNodes,
            workflow: editorWorkflow,
        });

        temporal().resume();

        const restore = preserveWorkflowDataStore();

        simulateReadOnlyCanvasUsingTheStore();

        restore();

        const {edges, isWorkflowLoaded, nodes, workflow} = useWorkflowDataStore.getState();

        expect(isWorkflowLoaded).toBe(true);
        expect(workflow).toBe(editorWorkflow);
        expect(nodes).toBe(editorNodes);
        expect(edges).toBe(editorEdges);
    });

    it('resets the store when no editor workflow was loaded, so the next read-only view starts clean', () => {
        const restore = preserveWorkflowDataStore();

        simulateReadOnlyCanvasUsingTheStore();

        restore();

        const {edges, isWorkflowLoaded, nodes} = useWorkflowDataStore.getState();

        expect(isWorkflowLoaded).toBe(false);
        expect(nodes).toEqual([]);
        expect(edges).toEqual([]);
    });

    it('does not add an undo step to the editor history', () => {
        temporal().pause();

        useWorkflowDataStore.setState({isWorkflowLoaded: true, nodes: editorNodes, workflow: editorWorkflow});

        temporal().resume();

        const restore = preserveWorkflowDataStore();

        simulateReadOnlyCanvasUsingTheStore();

        restore();

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(0);
    });

    it('does not add an undo step when falling back to a reset', () => {
        temporal().pause();

        useWorkflowDataStore.setState({workflow: editorWorkflow});

        temporal().resume();

        const restore = preserveWorkflowDataStore();

        restore();

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(0);
    });
});
