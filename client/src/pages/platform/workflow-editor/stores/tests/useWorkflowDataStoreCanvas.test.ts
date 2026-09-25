import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../useWorkflowDataStore';

const loadedGraphNodes = [{data: {}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'}] as Node[];
const loadedGraphEdges = [{id: 'trigger_1=>task_1', source: 'trigger_1', target: 'task_1'}] as Edge[];

describe('useWorkflowDataStore canvas', () => {
    it('starts with an empty canvas, so nothing is drawn before the workflow has been laid out', () => {
        const {edges, nodes} = useWorkflowDataStore.getState();

        expect(nodes).toEqual([]);
        expect(edges).toEqual([]);
    });

    it('empties the canvas on reset', () => {
        useWorkflowDataStore.setState({edges: loadedGraphEdges, nodes: loadedGraphNodes});

        useWorkflowDataStore.getState().reset();

        expect(useWorkflowDataStore.getState().nodes).toEqual([]);
        expect(useWorkflowDataStore.getState().edges).toEqual([]);
    });

    it('empties the canvas on clearCanvas', () => {
        useWorkflowDataStore.setState({edges: loadedGraphEdges, nodes: loadedGraphNodes});

        useWorkflowDataStore.getState().clearCanvas();

        expect(useWorkflowDataStore.getState().nodes).toEqual([]);
        expect(useWorkflowDataStore.getState().edges).toEqual([]);
    });
});
