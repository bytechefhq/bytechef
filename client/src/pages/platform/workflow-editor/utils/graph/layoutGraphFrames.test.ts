import {GRAPH_START_EDGE_TYPE, GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {toReadOnlyLayoutEdges, toReadOnlyLayoutNodes} from '../../hooks/useLayout';
import createGraphEdges from '../createGraphEdges';
import createGraphNode from '../createGraphNode';
import {getElkLayoutElements} from '../elkLayoutUtils';
import {GetLayoutElementsProps, LayoutElementsResultI, getDagreNodeSize, getLayoutElements} from '../layoutUtils';
import {
    GRAPH_FRAME_HEADER_HEIGHT,
    GRAPH_FRAME_PADDING,
    GRAPH_MEMBER_BOX_WIDTH,
    GRAPH_MEMBER_SPACING,
    GRAPH_START_SIZE,
    fromFrameChildPosition,
} from './graphFrameGeometry';
import {buildGraphMemberDragStopPositions, getGraphMemberGroupInset} from './graphMemberDrag';
import {findGraphMemberOwner, layoutGraphFrames} from './layoutGraphFrames';

const stackedLayout = async ({edges, nodes}: GetLayoutElementsProps): Promise<LayoutElementsResultI> => ({
    edges,
    engine: 'dagre' as const,
    nodes: nodes.map((node, nodeIndex) => ({...node, position: {x: 0, y: nodeIndex * 100}})),
});

type MemberSpecType = {name: string; position?: {x: number; y: number}};

interface CreateGraphDispatcherNodeProps {
    graphId: string;
    members: MemberSpecType[];
    nestedIn?: {graphId: string; index: number};
    transitions: Array<{condition?: string; from: string; to: string}>;
}

function createGraphDispatcherNode({graphId, members, nestedIn, transitions}: CreateGraphDispatcherNodeProps): Node {
    return {
        data: {
            componentName: 'graph',
            ...(nestedIn ? {graphData: nestedIn} : {}),
            parameters: {
                nodes: members.map((member) => ({
                    name: member.name,
                    ...(member.position ? {metadata: {ui: {nodePosition: member.position}}} : {}),
                    type: 'mailchimp/v1/subscribe',
                })),
                startNode: members[0]?.name,
                transitions,
            },
            taskDispatcher: true,
            taskDispatcherId: graphId,
            workflowNodeName: graphId,
        },
        id: graphId,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

interface CreateMemberTaskNodeProps {
    graphId: string;
    index: number;
    member: MemberSpecType;
}

function createMemberTaskNode({graphId, index, member}: CreateMemberTaskNodeProps): Node {
    return {
        data: {
            componentName: 'mailchimp',
            graphData: {graphId, index},
            ...(member.position ? {metadata: {ui: {nodePosition: member.position}}} : {}),
            workflowNodeName: member.name,
        },
        id: member.name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

interface BuildGraphFixtureProps {
    graphId: string;
    members: MemberSpecType[];
    transitions?: Array<{condition?: string; from: string; to: string}>;
}

function buildGraphFixture({graphId, members, transitions = []}: BuildGraphFixtureProps): {
    edges: Edge[];
    nodes: Node[];
} {
    const dispatcherNode = createGraphDispatcherNode({graphId, members, transitions});

    const nodes = [
        ...createGraphNode({allNodes: [dispatcherNode], graphId}),
        ...members.map((member, memberIndex) => createMemberTaskNode({graphId, index: memberIndex, member})),
    ];

    return {edges: createGraphEdges(dispatcherNode), nodes};
}

function findNode(nodes: Node[], id: string): Node {
    const found = nodes.find((node) => node.id === id);

    expect(found, `expected a node with id ${id}`).toBeDefined();

    return found!;
}

function nodesById(nodes: Node[]): Map<string, Node> {
    return new Map(nodes.map((node) => [node.id, node]));
}

describe('findGraphMemberOwner', () => {
    it('returns the owning graph for a direct member task', () => {
        const {nodes} = buildGraphFixture({graphId: 'graph_1', members: [{name: 'task_0'}]});

        expect(findGraphMemberOwner(findNode(nodes, 'task_0'), nodesById(nodes))).toBe('graph_1');
    });

    it('returns the owning graph for a node in a member subtree', () => {
        const {nodes} = buildGraphFixture({graphId: 'graph_1', members: [{name: 'loop_1'}]});

        const subtreeNode: Node = {
            data: {componentName: 'mailchimp', loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner'},
            id: 'inner',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        expect(findGraphMemberOwner(subtreeNode, nodesById([...nodes, subtreeNode]))).toBe('graph_1');
    });

    it('returns undefined for the graph dispatcher node and its own frame', () => {
        const {nodes} = buildGraphFixture({graphId: 'graph_1', members: [{name: 'task_0'}]});

        expect(findGraphMemberOwner(findNode(nodes, 'graph_1'), nodesById(nodes))).toBeUndefined();
        expect(findGraphMemberOwner(findNode(nodes, 'graph_1-graph-frame'), nodesById(nodes))).toBeUndefined();
    });
});

describe('layoutGraphFrames', () => {
    it('leaves a graph-free workflow untouched', async () => {
        const nodes: Node[] = [
            {data: {componentName: 'mailchimp'}, id: 'task_0', position: {x: 0, y: 0}, type: 'workflow'},
        ];
        const edges: Edge[] = [];

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        expect(framed.outerNodes).toBe(nodes);
        expect(framed.outerEdges).toBe(edges);
        expect(framed.memberNodes).toEqual([]);
        expect(framed.autoPlaced).toEqual({});
    });

    it('pushes members out from under a member whose box has grown over them', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [
                {name: 'task_0', position: {x: 100, y: 0}},
                {name: 'task_1', position: {x: 140, y: 340}},
                {name: 'task_2', position: {x: 140, y: 560}},
            ],
            transitions: [
                {from: 'task_0', to: 'task_1'},
                {from: 'task_1', to: 'task_2'},
            ],
        });

        const expandedMember = findNode(nodes, 'task_0');

        expandedMember.data = {
            ...expandedMember.data,
            clusterFrame: {clusterRootId: 'task_0', height: 800, width: 900},
        };

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        expect(fromFrameChildPosition(findNode(framed.memberNodes, 'task_0').position)).toEqual({x: 100, y: 0});
        expect(fromFrameChildPosition(findNode(framed.memberNodes, 'task_1').position)).toEqual({
            x: 140,
            y: 800 + GRAPH_MEMBER_SPACING,
        });
        expect(fromFrameChildPosition(findNode(framed.memberNodes, 'task_2').position).y).toBeGreaterThan(
            800 + GRAPH_MEMBER_SPACING
        );
    });

    it('parents members to the frame at their saved positions and sizes the frame around them', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [
                {name: 'task_0', position: {x: 0, y: 0}},
                {name: 'task_1', position: {x: 300, y: 0}},
            ],
            transitions: [{from: 'task_0', to: 'task_1'}],
        });

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        const firstMember = findNode(framed.memberNodes, 'task_0');
        const secondMember = findNode(framed.memberNodes, 'task_1');

        expect(firstMember.parentId).toBe('graph_1-graph-frame');
        expect(secondMember.parentId).toBe('graph_1-graph-frame');

        expect(firstMember.position).toEqual({x: 0, y: GRAPH_FRAME_HEADER_HEIGHT});
        expect(secondMember.position).toEqual({x: 300, y: GRAPH_FRAME_HEADER_HEIGHT});

        expect(firstMember.draggable).toBe(true);
        expect(firstMember.extent).toEqual([
            [0, GRAPH_FRAME_HEADER_HEIGHT],
            [Infinity, Infinity],
        ]);

        const frameNode = findNode(framed.outerNodes, 'graph_1-graph-frame');

        expect(frameNode.data.graphFrame).toMatchObject({graphId: 'graph_1', height: 200, width: 444});
        expect(frameNode.width).toBe(444);
        expect(frameNode.height).toBe(200);

        const outerNodeIds = framed.outerNodes.map((node) => node.id);

        expect(outerNodeIds).toEqual(['graph_1', 'graph_1-graph-frame']);

        expect(framed.memberNodes.map((node) => node.id).sort()).toEqual([
            'graph_1-graph-placeholder',
            'graph_1-graph-start',
            'task_0',
            'task_1',
        ]);

        expect(framed.autoPlaced).toEqual({});
    });

    it('auto-places a member that has no saved position', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [{name: 'task_0', position: {x: 0, y: 0}}, {name: 'task_1'}],
            transitions: [{from: 'task_0', to: 'task_1'}],
        });

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        expect(Object.keys(framed.autoPlaced)).toEqual(['graph_1']);
        expect(Object.keys(framed.autoPlaced.graph_1)).toEqual(['task_1']);

        const autoPlacedPosition = framed.autoPlaced.graph_1.task_1;

        expect(autoPlacedPosition.y).toBeGreaterThan(0);

        expect(findNode(framed.memberNodes, 'task_1').position).toEqual({
            x: autoPlacedPosition.x,
            y: autoPlacedPosition.y + GRAPH_FRAME_HEADER_HEIGHT,
        });

        expect(findNode(framed.memberNodes, 'task_0').position).toEqual({x: 0, y: GRAPH_FRAME_HEADER_HEIGHT});
    });

    it('clears the Start pill when every member has to be auto-placed', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [{name: 'task_0'}, {name: 'task_1'}],
            transitions: [{from: 'task_0', to: 'task_1'}],
        });

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        for (const memberName of ['task_0', 'task_1']) {
            const memberContentTop = fromFrameChildPosition(findNode(framed.memberNodes, memberName).position).y;

            expect(memberContentTop).toBeGreaterThanOrEqual(GRAPH_START_SIZE.height + GRAPH_FRAME_PADDING);
        }

        const startPosition = findNode(framed.memberNodes, 'graph_1-graph-start').position;
        const entryPosition = findNode(framed.memberNodes, 'task_0').position;

        expect(startPosition.y).toBe(GRAPH_FRAME_HEADER_HEIGHT);
        expect(startPosition.x + GRAPH_START_SIZE.width / 2).toBe(entryPosition.x + GRAPH_MEMBER_BOX_WIDTH / 2);
    });

    it('centres the Start pill down the left edge in LR', async () => {
        const {edges, nodes} = buildGraphFixture({graphId: 'graph_1', members: [{name: 'task_0'}], transitions: []});

        const framed = await layoutGraphFrames({direction: 'LR', edges, layoutFunction: stackedLayout, nodes});

        const startNode = findNode(framed.memberNodes, 'graph_1-graph-start');
        const entryNode = findNode(framed.memberNodes, 'task_0');

        expect(startNode.position.x).toBe(GRAPH_FRAME_PADDING);
        expect(startNode.position.y + GRAPH_START_SIZE.height / 2).toBe(
            entryNode.position.y + GRAPH_MEMBER_BOX_WIDTH / 2
        );
    });

    it('keeps the Start pill of an empty graph on the left, level with the chain, in LR', async () => {
        const {edges, nodes} = buildGraphFixture({graphId: 'graph_1', members: [], transitions: []});

        const framed = await layoutGraphFrames({direction: 'LR', edges, layoutFunction: stackedLayout, nodes});

        const frameNode = findNode(framed.outerNodes, 'graph_1-graph-frame');
        const startNode = findNode(framed.memberNodes, 'graph_1-graph-start');

        expect(startNode.position.x).toBe(GRAPH_FRAME_PADDING);
        expect(startNode.position.y + GRAPH_START_SIZE.height / 2).toBe(frameNode.height! / 2);
    });

    it('keeps the Start pill of an empty graph at the top, centred on the chain, in TB', async () => {
        const {edges, nodes} = buildGraphFixture({graphId: 'graph_1', members: [], transitions: []});

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        const frameNode = findNode(framed.outerNodes, 'graph_1-graph-frame');
        const startNode = findNode(framed.memberNodes, 'graph_1-graph-start');

        expect(startNode.position.x + GRAPH_START_SIZE.width / 2).toBe(frameNode.width! / 2);
        expect(startNode.position.y).toBe(GRAPH_FRAME_HEADER_HEIGHT);
    });

    it('keeps members auto-arranged into one LR column where they were placed', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [
                {name: 'task_0', position: {x: 132, y: 72}},
                {name: 'task_1', position: {x: 132, y: 184}},
                {name: 'task_2', position: {x: 284, y: 172}},
            ],
            transitions: [
                {from: 'task_0', to: 'task_2'},
                {from: 'task_1', to: 'task_2'},
            ],
        });

        const framed = await layoutGraphFrames({direction: 'LR', edges, layoutFunction: stackedLayout, nodes});

        expect(fromFrameChildPosition(findNode(framed.memberNodes, 'task_0').position)).toEqual({x: 132, y: 72});
        expect(fromFrameChildPosition(findNode(framed.memberNodes, 'task_1').position)).toEqual({x: 132, y: 184});
        expect(fromFrameChildPosition(findNode(framed.memberNodes, 'task_2').position)).toEqual({x: 284, y: 172});
    });

    it('moves transition and start edges onto the member edge list', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [
                {name: 'task_0', position: {x: 0, y: 0}},
                {name: 'task_1', position: {x: 300, y: 0}},
            ],
            transitions: [{from: 'task_0', to: 'task_1'}],
        });

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        expect(framed.memberEdges.map((edge) => edge.type).sort()).toEqual([
            GRAPH_START_EDGE_TYPE,
            GRAPH_TRANSITION_EDGE_TYPE,
        ]);

        expect(framed.outerEdges.some((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toBe(false);
        expect(framed.outerEdges.some((edge) => edge.type === GRAPH_START_EDGE_TYPE)).toBe(false);

        expect(framed.outerEdges.map((edge) => `${edge.source}=>${edge.target}`)).toEqual([
            'graph_1=>graph_1-graph-frame',
        ]);
    });

    it('drops an edge that would escape the frame', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [{name: 'task_0', position: {x: 0, y: 0}}],
        });

        const escapingEdge: Edge = {id: 'task_0=>after', source: 'task_0', target: 'after', type: 'workflow'};

        const afterNode: Node = {
            data: {componentName: 'mailchimp'},
            id: 'after',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const framed = await layoutGraphFrames({
            direction: 'TB',
            edges: [...edges, escapingEdge],
            layoutFunction: stackedLayout,
            nodes: [...nodes, afterNode],
        });

        expect(framed.outerEdges).not.toContain(escapingEdge);
        expect(framed.memberEdges).not.toContain(escapingEdge);
        expect(framed.outerNodes.map((node) => node.id)).toContain('after');
    });

    it('lays a dispatcher member out as one block and keeps its subtree inside the frame', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [{name: 'loop_1', position: {x: 10, y: 20}}],
        });

        const loopMemberNode = findNode(nodes, 'loop_1');

        loopMemberNode.data = {...loopMemberNode.data, componentName: 'loop', taskDispatcherId: 'loop_1'};

        const loopChildNode: Node = {
            data: {componentName: 'mailchimp', loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner'},
            id: 'inner',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const framed = await layoutGraphFrames({
            direction: 'TB',
            edges: [...edges, {id: 'loop_1=>inner', source: 'loop_1', target: 'inner', type: 'workflow'}],
            layoutFunction: stackedLayout,
            nodes: [...nodes, loopChildNode],
        });

        expect(findNode(framed.memberNodes, 'loop_1').position).toEqual({x: 10, y: 20 + GRAPH_FRAME_HEADER_HEIGHT});
        expect(findNode(framed.memberNodes, 'inner').position).toEqual({
            x: 10,
            y: 120 + GRAPH_FRAME_HEADER_HEIGHT,
        });

        expect(findNode(framed.memberNodes, 'inner').parentId).toBe('graph_1-graph-frame');

        expect(framed.memberEdges.map((edge) => edge.id)).toContain('loop_1=>inner');

        expect(findNode(framed.outerNodes, 'graph_1-graph-frame').data.graphFrame).toMatchObject({
            graphId: 'graph_1',
            height: 284,
            width: 322,
        });
    });

    it('keeps a dragged dispatcher member where it was dropped across a save and a relayout', async () => {
        const headerInsetLayout = async ({edges, nodes}: GetLayoutElementsProps): Promise<LayoutElementsResultI> => ({
            edges,
            engine: 'dagre' as const,
            nodes: nodes.map((node, nodeIndex) => ({
                ...node,
                position: nodeIndex === 0 ? {x: 50, y: 0} : {x: 0, y: nodeIndex * 100},
            })),
        });

        const layOutLoopMemberAt = async (savedPosition: {x: number; y: number}) => {
            const {edges, nodes} = buildGraphFixture({
                graphId: 'graph_1',
                members: [{name: 'loop_1', position: savedPosition}],
            });

            const loopMemberNode = findNode(nodes, 'loop_1');

            loopMemberNode.data = {...loopMemberNode.data, componentName: 'loop', taskDispatcherId: 'loop_1'};

            const loopChildNode: Node = {
                data: {componentName: 'mailchimp', loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner'},
                id: 'inner',
                position: {x: 0, y: 0},
                type: 'workflow',
            };

            const framed = await layoutGraphFrames({
                direction: 'TB',
                edges: [...edges, {id: 'loop_1=>inner', source: 'loop_1', target: 'inner', type: 'workflow'}],
                layoutFunction: headerInsetLayout,
                nodes: [...nodes, loopChildNode],
            });

            return framed.memberNodes;
        };

        const laidOutNodes = await layOutLoopMemberAt({x: 10, y: 20});

        const laidOutHeaderPosition = findNode(laidOutNodes, 'loop_1').position;

        const droppedHeaderPosition = {x: laidOutHeaderPosition.x + 40, y: laidOutHeaderPosition.y + 60};

        const draggedNodes = laidOutNodes.map((node) => ({
            ...node,
            position: {x: node.position.x + 40, y: node.position.y + 60},
        }));

        const savedPositions = buildGraphMemberDragStopPositions({
            draggedNodeId: 'loop_1',
            draggedNodePosition: droppedHeaderPosition,
            memberGroupInset: getGraphMemberGroupInset({graphId: 'graph_1', memberId: 'loop_1', nodes: draggedNodes}),
        });

        const relaidOutNodes = await layOutLoopMemberAt(savedPositions.loop_1);

        expect(findNode(relaidOutNodes, 'loop_1').position).toEqual(droppedHeaderPosition);
    });

    it('processes a nested graph first and treats its frame as a leaf of the outer member group', async () => {
        const outerDispatcherNode = createGraphDispatcherNode({
            graphId: 'graph_1',
            members: [{name: 'graph_2', position: {x: 0, y: 0}}],
            transitions: [],
        });

        const innerDispatcherNode = createGraphDispatcherNode({
            graphId: 'graph_2',
            members: [{name: 'task_a', position: {x: 0, y: 0}}],
            nestedIn: {graphId: 'graph_1', index: 0},
            transitions: [],
        });

        const nodes: Node[] = [
            ...createGraphNode({allNodes: [outerDispatcherNode], graphId: 'graph_1'}),
            ...createGraphNode({allNodes: [innerDispatcherNode], graphId: 'graph_2', isNested: true}),
            createMemberTaskNode({graphId: 'graph_2', index: 0, member: {name: 'task_a', position: {x: 0, y: 0}}}),
        ];

        const edges: Edge[] = [...createGraphEdges(outerDispatcherNode), ...createGraphEdges(innerDispatcherNode)];

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction: stackedLayout, nodes});

        expect(framed.outerNodes.map((node) => node.id)).toEqual(['graph_1', 'graph_1-graph-frame']);

        expect(findNode(framed.memberNodes, 'graph_2-graph-frame').parentId).toBe('graph_1-graph-frame');
        expect(findNode(framed.memberNodes, 'task_a').parentId).toBe('graph_2-graph-frame');

        const memberNodeIds = framed.memberNodes.map((node) => node.id);

        expect(memberNodeIds.indexOf('graph_2-graph-frame')).toBeLessThan(memberNodeIds.indexOf('task_a'));

        const innerFrameSize = findNode(framed.memberNodes, 'graph_2-graph-frame').data.graphFrame as {
            height: number;
            width: number;
        };
        const outerFrameSize = findNode(framed.outerNodes, 'graph_1-graph-frame').data.graphFrame as {
            height: number;
            width: number;
        };

        expect(innerFrameSize.width).toBe(320);
        expect(outerFrameSize.width).toBeGreaterThanOrEqual(innerFrameSize.width);
    });
});

describe('layoutGraphFrames engine parity', () => {
    const buildParityFixture = () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [
                {name: 'task_0', position: {x: 0, y: 0}},
                {name: 'loop_1', position: {x: 300, y: 0}},
                {name: 'task_2', position: {x: 150, y: 320}},
            ],
            transitions: [
                {from: 'task_0', to: 'loop_1'},
                {condition: '${loop_1.ok}', from: 'loop_1', to: 'task_2'},
            ],
        });

        const loopMemberNode = findNode(nodes, 'loop_1');

        loopMemberNode.data = {...loopMemberNode.data, componentName: 'loop', taskDispatcherId: 'loop_1'};

        const loopSubtreeNodes: Node[] = [
            {
                data: {loopId: 'loop_1', taskDispatcherId: 'loop_1'},
                id: 'loop_1-loop-top-ghost',
                position: {x: 0, y: 0},
                type: 'taskDispatcherTopGhostNode',
            },
            {
                data: {componentName: 'mailchimp', loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner'},
                id: 'inner',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
            {
                data: {taskDispatcherId: 'loop_1'},
                id: 'loop_1-loop-bottom-ghost',
                position: {x: 0, y: 0},
                type: 'taskDispatcherBottomGhostNode',
            },
        ];

        const loopSubtreeEdges: Edge[] = [
            {id: 'loop_1=>loop_1-loop-top-ghost', source: 'loop_1', target: 'loop_1-loop-top-ghost', type: 'workflow'},
            {id: 'loop_1-loop-top-ghost=>inner', source: 'loop_1-loop-top-ghost', target: 'inner', type: 'workflow'},
            {
                id: 'inner=>loop_1-loop-bottom-ghost',
                source: 'inner',
                target: 'loop_1-loop-bottom-ghost',
                type: 'workflow',
            },
        ];

        return {edges: [...edges, ...loopSubtreeEdges], nodes: [...nodes, ...loopSubtreeNodes]};
    };

    it('actually runs ELK rather than silently falling back to dagre', async () => {
        const {edges, nodes} = buildParityFixture();

        const elkResult = await getElkLayoutElements({canvasWidth: 1200, direction: 'TB', edges, nodes});

        expect(elkResult.engine).toBe('elk');
    });

    it('puts every member at the same frame-relative position under dagre and ELK', async () => {
        const dagreFixture = buildParityFixture();
        const elkFixture = buildParityFixture();

        const dagreFramed = await layoutGraphFrames({
            direction: 'TB',
            edges: dagreFixture.edges,
            layoutFunction: getLayoutElements,
            nodes: dagreFixture.nodes,
        });
        const elkFramed = await layoutGraphFrames({
            direction: 'TB',
            edges: elkFixture.edges,
            layoutFunction: getElkLayoutElements,
            nodes: elkFixture.nodes,
        });

        for (const memberName of ['task_0', 'loop_1', 'task_2']) {
            expect(findNode(elkFramed.memberNodes, memberName).position).toEqual(
                findNode(dagreFramed.memberNodes, memberName).position
            );
        }
    });

    it.each([
        ['dagre', getLayoutElements],
        ['elk', getElkLayoutElements],
    ])('sizes the frame to contain every one of its children under %s', async (_engineName, layoutFunction) => {
        const {edges, nodes} = buildParityFixture();

        const framed = await layoutGraphFrames({direction: 'TB', edges, layoutFunction, nodes});

        const frameSize = findNode(framed.outerNodes, 'graph_1-graph-frame').data.graphFrame as {
            height: number;
            width: number;
        };

        for (const memberNode of framed.memberNodes) {
            if (memberNode.id.endsWith('-graph-placeholder')) {
                continue;
            }

            const size = getDagreNodeSize(memberNode, 'TB');

            expect(memberNode.position.x).toBeGreaterThanOrEqual(0);
            expect(memberNode.position.y).toBeGreaterThanOrEqual(GRAPH_FRAME_HEADER_HEIGHT);
            expect(memberNode.position.x + size.width).toBeLessThanOrEqual(frameSize.width);
            expect(memberNode.position.y + size.height).toBeLessThanOrEqual(frameSize.height);
        }
    });
});

describe('read-only graph frames', () => {
    it('routes a read-only graph transition and start edge into the frame with their types intact', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [{name: 'task_0'}, {name: 'task_1'}],
            transitions: [{from: 'task_0', to: 'task_1'}],
        });

        const framed = await layoutGraphFrames({
            direction: 'TB',
            edges: toReadOnlyLayoutEdges(edges),
            layoutFunction: stackedLayout,
            nodes,
        });

        const memberEdgeTypes = framed.memberEdges.map((edge) => edge.type);

        expect(memberEdgeTypes).toContain(GRAPH_TRANSITION_EDGE_TYPE);
        expect(memberEdgeTypes).toContain(GRAPH_START_EDGE_TYPE);

        const transitionEdge = framed.memberEdges.find((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)!;

        expect(transitionEdge.source).toBe('task_0');
        expect(transitionEdge.target).toBe('task_1');
        expect(transitionEdge.sourceHandle).toBe('task_0-graph-transition-source');
        expect(transitionEdge.targetHandle).toBe('task_1-graph-transition-target');
    });

    it('leaves the members of a read-only graph undraggable once the pre-pass has stamped them', async () => {
        const {edges, nodes} = buildGraphFixture({
            graphId: 'graph_1',
            members: [{name: 'task_0'}, {name: 'task_1'}],
            transitions: [{from: 'task_0', to: 'task_1'}],
        });

        const framed = await layoutGraphFrames({
            direction: 'TB',
            edges: toReadOnlyLayoutEdges(edges),
            layoutFunction: stackedLayout,
            nodes,
        });

        expect(framed.memberNodes.filter((node) => node.draggable).length).toBeGreaterThan(0);

        expect(toReadOnlyLayoutNodes(framed.memberNodes).some((node) => node.draggable)).toBe(false);
    });
});
