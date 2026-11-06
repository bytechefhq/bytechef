import {type Edge, type Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';
import orderExecutedTriggerFanInEdgesLast from './orderExecutedTriggerFanInEdgesLast';

const createNode = (id: string): Node => ({data: {workflowNodeName: id}, id, position: {x: 0, y: 0}});

const createFanInEdge = (source: string, target: string): Edge => ({
    data: {triggerFanIn: true},
    id: `${source}=>${target}`,
    source,
    target,
});

const nodes = [createNode('trigger_1'), createNode('trigger_2'), createNode('logger_1')];

const fanInEdges = [createFanInEdge('trigger_1', 'logger_1'), createFanInEdge('trigger_2', 'logger_1')];

const completedState = {status: 'COMPLETED'} as WorkflowTestNodeStateI;

const orderEdgeIds = (edges: Edge[], workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>) =>
    orderExecutedTriggerFanInEdgesLast({
        disabledTaskNames: new Set<string>(),
        edges,
        nodes,
        workflowTestNodeStates,
    }).map((edge) => edge.id);

describe('orderExecutedTriggerFanInEdgesLast', () => {
    it('should move the executed trigger fan-in edge after its siblings', () => {
        const edgeIds = orderEdgeIds(fanInEdges, {logger_1: completedState, trigger_1: completedState});

        expect(edgeIds).toEqual(['trigger_2=>logger_1', 'trigger_1=>logger_1']);
    });

    it('should keep the order when the executed trigger edge is already last', () => {
        const edgeIds = orderEdgeIds(fanInEdges, {logger_1: completedState, trigger_2: completedState});

        expect(edgeIds).toEqual(['trigger_1=>logger_1', 'trigger_2=>logger_1']);
    });

    it('should return the same edges array when nothing has executed', () => {
        const orderedEdges = orderExecutedTriggerFanInEdgesLast({
            disabledTaskNames: new Set<string>(),
            edges: fanInEdges,
            nodes,
            workflowTestNodeStates: {},
        });

        expect(orderedEdges).toBe(fanInEdges);
    });

    it('should not move executed edges that are not trigger fan-in edges', () => {
        const taskEdge: Edge = {id: 'logger_1=>logger_2', source: 'logger_1', target: 'logger_2'};
        const edges = [taskEdge, ...fanInEdges];

        const edgeIds = orderExecutedTriggerFanInEdgesLast({
            disabledTaskNames: new Set<string>(),
            edges,
            nodes: [...nodes, createNode('logger_2')],
            workflowTestNodeStates: {logger_1: completedState, logger_2: completedState},
        }).map((edge) => edge.id);

        expect(edgeIds).toEqual(['logger_1=>logger_2', 'trigger_1=>logger_1', 'trigger_2=>logger_1']);
    });
});
