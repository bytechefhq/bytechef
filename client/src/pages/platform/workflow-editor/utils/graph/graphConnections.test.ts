import {GRAPH_START_NODE_TYPE} from '@/shared/constants';
import {GraphTransitionType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    buildGraphConnectionParameters,
    deriveGraphPendingConnection,
    isValidGraphConnection,
    resolveGraphConnection,
} from './graphConnections';
import {GRAPH_FRAME_HEADER_HEIGHT} from './graphFrameGeometry';

function buildMemberNode(name: string, graphId = 'graph_1'): Node {
    return {data: {graphData: {graphId, index: 0}, name}, id: name, position: {x: 0, y: 0}, type: 'workflow'};
}

function buildStartNode(graphId = 'graph_1'): Node {
    return {
        data: {graphStart: {graphId}, name: `${graphId}-graph-start`},
        id: `${graphId}-graph-start`,
        position: {x: 0, y: 0},
        type: GRAPH_START_NODE_TYPE,
    };
}

function buildDispatcherNode(graphId = 'graph_1', transitions: GraphTransitionType[] = []): Node {
    return {
        data: {name: graphId, parameters: {nodes: [], transitions}, taskDispatcher: true},
        id: graphId,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

function buildNodesById(nodes: Node[]): Map<string, Node> {
    return new Map(nodes.map((node) => [node.id, node]));
}

const DEFAULT_NODES_BY_ID = buildNodesById([
    buildDispatcherNode(),
    buildStartNode(),
    buildMemberNode('task_1'),
    buildMemberNode('task_2'),
    buildMemberNode('other_1', 'graph_2'),
    buildDispatcherNode('graph_2'),
]);

function memberConnection(source: string, target: string) {
    return {
        source,
        sourceHandle: `${source}-graph-transition-source`,
        target,
        targetHandle: `${target}-graph-transition-target`,
    };
}

describe('resolveGraphConnection', () => {
    it('resolves a connection between two members of the same graph', () => {
        expect(resolveGraphConnection(memberConnection('task_1', 'task_2'), DEFAULT_NODES_BY_ID)).toEqual({
            from: 'task_1',
            graphId: 'graph_1',
            kind: 'transition',
            to: 'task_2',
        });
    });

    it('resolves the start pill connection', () => {
        expect(
            resolveGraphConnection(
                {
                    source: 'graph_1-graph-start',
                    sourceHandle: 'graph_1-graph-start-source',
                    target: 'task_2',
                    targetHandle: 'task_2-graph-transition-target',
                },
                DEFAULT_NODES_BY_ID
            )
        ).toEqual({graphId: 'graph_1', kind: 'start', to: 'task_2'});
    });
});

describe('isValidGraphConnection', () => {
    it('accepts a self-loop', () => {
        expect(isValidGraphConnection(memberConnection('task_1', 'task_1'), DEFAULT_NODES_BY_ID)).toBe(true);
    });

    it('rejects a connection across two graphs', () => {
        expect(isValidGraphConnection(memberConnection('task_1', 'other_1'), DEFAULT_NODES_BY_ID)).toBe(false);
    });

    it('rejects a start pill reaching into another graph', () => {
        expect(
            isValidGraphConnection(
                {
                    source: 'graph_1-graph-start',
                    sourceHandle: 'graph_1-graph-start-source',
                    target: 'other_1',
                    targetHandle: 'other_1-graph-transition-target',
                },
                DEFAULT_NODES_BY_ID
            )
        ).toBe(false);
    });

    it('rejects a target handle that is not a transition target', () => {
        expect(
            isValidGraphConnection(
                {
                    source: 'task_1',
                    sourceHandle: 'task_1-graph-transition-source',
                    target: 'task_2',
                    targetHandle: 'task_2-graph-transition-dynamic',
                },
                DEFAULT_NODES_BY_ID
            )
        ).toBe(false);
    });

    it('rejects a source handle that is not a transition source', () => {
        expect(
            isValidGraphConnection(
                {
                    source: 'task_1',
                    sourceHandle: 'task_1-bottom',
                    target: 'task_2',
                    targetHandle: 'task_2-graph-transition-target',
                },
                DEFAULT_NODES_BY_ID
            )
        ).toBe(false);
    });

    it('rejects a node that is not a graph member', () => {
        const nodesById = buildNodesById([
            buildDispatcherNode(),
            buildMemberNode('task_1'),
            {data: {name: 'plain_1'}, id: 'plain_1', position: {x: 0, y: 0}, type: 'workflow'},
        ]);

        expect(isValidGraphConnection(memberConnection('task_1', 'plain_1'), nodesById)).toBe(false);
    });

    it('rejects a transition that already exists', () => {
        const nodesById = buildNodesById([
            buildDispatcherNode('graph_1', [{from: 'task_1', to: 'task_2'}]),
            buildMemberNode('task_1'),
            buildMemberNode('task_2'),
        ]);

        expect(isValidGraphConnection(memberConnection('task_1', 'task_2'), nodesById)).toBe(false);
        expect(isValidGraphConnection(memberConnection('task_2', 'task_1'), nodesById)).toBe(true);
    });
});

describe('buildGraphConnectionParameters', () => {
    it('appends the transition to the graph parameters', () => {
        expect(
            buildGraphConnectionParameters({
                graphConnection: {from: 'task_1', graphId: 'graph_1', kind: 'transition', to: 'task_2'},
                parameters: {
                    nodes: [{name: 'task_1'}, {name: 'task_2'}],
                    transitions: [{from: 'task_2', to: 'task_1'}],
                },
            })
        ).toEqual({
            nodes: [{name: 'task_1'}, {name: 'task_2'}],
            transitions: [
                {from: 'task_2', to: 'task_1'},
                {from: 'task_1', to: 'task_2'},
            ],
        });
    });

    it('sets the start node without touching the transitions', () => {
        expect(
            buildGraphConnectionParameters({
                graphConnection: {graphId: 'graph_1', kind: 'start', to: 'task_2'},
                parameters: {startNode: 'task_1', transitions: [{from: 'task_1', to: 'task_2'}]},
            })
        ).toEqual({startNode: 'task_2', transitions: [{from: 'task_1', to: 'task_2'}]});
    });

    it('flushes pending auto-placed member positions in the same update', () => {
        expect(
            buildGraphConnectionParameters({
                autoPlacedPositions: {task_2: {x: 400, y: 60}},
                graphConnection: {from: 'task_1', graphId: 'graph_1', kind: 'transition', to: 'task_2'},
                parameters: {
                    nodes: [{metadata: {ui: {nodePosition: {x: 10, y: 20}}}, name: 'task_1'}, {name: 'task_2'}],
                    transitions: [],
                },
            })
        ).toEqual({
            nodes: [
                {metadata: {ui: {nodePosition: {x: 10, y: 20}}}, name: 'task_1'},
                {metadata: {ui: {nodePosition: {x: 400, y: 60}}}, name: 'task_2'},
            ],
            transitions: [{from: 'task_1', to: 'task_2'}],
        });
    });
});

describe('deriveGraphPendingConnection', () => {
    const BASE_INPUT = {
        flowPosition: {x: 320, y: 260},
        frameAbsolutePosition: {x: 100, y: 100},
        fromGraphId: 'graph_1',
        fromHandleType: 'source' as const,
        fromNodeName: 'task_1',
        hitFrameGraphId: 'graph_1',
        isValid: false,
    };

    it('converts the drop point into a content-origin position', () => {
        expect(deriveGraphPendingConnection(BASE_INPUT)).toEqual({
            dropPosition: {x: 220, y: 160 - GRAPH_FRAME_HEADER_HEIGHT},
            from: 'task_1',
            graphId: 'graph_1',
        });
    });

    it('clamps a drop inside the header band to the content origin', () => {
        expect(deriveGraphPendingConnection({...BASE_INPUT, flowPosition: {x: 90, y: 110}})?.dropPosition).toEqual({
            x: 0,
            y: 0,
        });
    });

    it('returns nothing when the connection already landed on a handle', () => {
        expect(deriveGraphPendingConnection({...BASE_INPUT, isValid: true})).toBeUndefined();
    });

    it('returns nothing when the pointer is outside any frame', () => {
        expect(deriveGraphPendingConnection({...BASE_INPUT, hitFrameGraphId: undefined})).toBeUndefined();
    });

    it('returns nothing when the pointer is over a different graph frame', () => {
        expect(deriveGraphPendingConnection({...BASE_INPUT, hitFrameGraphId: 'graph_2'})).toBeUndefined();
    });

    it('returns nothing when the drag did not start on a graph member', () => {
        expect(deriveGraphPendingConnection({...BASE_INPUT, fromGraphId: undefined})).toBeUndefined();
    });

    // `from` is the member the transition LEAVES, so a backwards drag out of a target handle would
    // author the edge pointing the opposite way from the one the user drew.
    it('returns nothing for a drag that started at a target handle', () => {
        expect(deriveGraphPendingConnection({...BASE_INPUT, fromHandleType: 'target'})).toBeUndefined();
        expect(deriveGraphPendingConnection({...BASE_INPUT, fromHandleType: undefined})).toBeUndefined();
    });
});
