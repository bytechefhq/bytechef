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
    GRAPH_START_SIZE,
    fromFrameChildPosition,
} from './graphFrameGeometry';
import {findGraphMemberOwner, layoutGraphFrames} from './layoutGraphFrames';

/**
 * Stacks every node it is handed 100px apart on the y axis. Deterministic and engine-free, so the
 * assertions below measure `layoutGraphFrames`'s own arithmetic (bbox normalisation, the
 * content-origin offset, the frame union) rather than dagre's or ELK's.
 */
const stackedLayout = async ({edges, nodes}: GetLayoutElementsProps): Promise<LayoutElementsResultI> => ({
    edges,
    engine: 'dagre' as const,
    nodes: nodes.map((node, nodeIndex) => ({...node, position: {x: 0, y: nodeIndex * 100}})),
});

type MemberSpecType = {name: string; position?: {x: number; y: number}};

function createGraphDispatcherNode(
    graphId: string,
    members: MemberSpecType[],
    transitions: Array<{condition?: string; from: string; to: string}>,
    nestedIn?: {graphId: string; index: number}
): Node {
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

function createMemberTaskNode(graphId: string, member: MemberSpecType, index: number): Node {
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

/**
 * Mirrors what `useLayout` assembles for a single top-level graph: the dispatcher node, the
 * auxiliary nodes `createGraphNode` splices in, and the member task nodes (a member IS a plain
 * task node, carrying only `graphData` to say which graph owns it).
 */
function buildGraphFixture(
    graphId: string,
    members: MemberSpecType[],
    transitions: Array<{condition?: string; from: string; to: string}> = []
): {edges: Edge[]; nodes: Node[]} {
    const dispatcherNode = createGraphDispatcherNode(graphId, members, transitions);

    const nodes = [
        ...createGraphNode({allNodes: [dispatcherNode], graphId}),
        ...members.map((member, memberIndex) => createMemberTaskNode(graphId, member, memberIndex)),
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
        const {nodes} = buildGraphFixture('graph_1', [{name: 'task_0'}]);

        expect(findGraphMemberOwner(findNode(nodes, 'task_0'), nodesById(nodes))).toBe('graph_1');
    });

    it('returns the owning graph for a node in a member subtree', () => {
        const {nodes} = buildGraphFixture('graph_1', [{name: 'loop_1'}]);

        const subtreeNode: Node = {
            data: {componentName: 'mailchimp', loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner'},
            id: 'inner',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        expect(findGraphMemberOwner(subtreeNode, nodesById([...nodes, subtreeNode]))).toBe('graph_1');
    });

    it('returns undefined for the graph dispatcher node and its own frame', () => {
        const {nodes} = buildGraphFixture('graph_1', [{name: 'task_0'}]);

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

        const framed = await layoutGraphFrames(nodes, edges, 'TB', stackedLayout);

        expect(framed.outerNodes).toBe(nodes);
        expect(framed.outerEdges).toBe(edges);
        expect(framed.memberNodes).toEqual([]);
        expect(framed.autoPlaced).toEqual({});
    });

    it('parents members to the frame at their saved positions and sizes the frame around them', async () => {
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [
                {name: 'task_0', position: {x: 0, y: 0}},
                {name: 'task_1', position: {x: 300, y: 0}},
            ],
            [{from: 'task_0', to: 'task_1'}]
        );

        const framed = await layoutGraphFrames(nodes, edges, 'TB', stackedLayout);

        const firstMember = findNode(framed.memberNodes, 'task_0');
        const secondMember = findNode(framed.memberNodes, 'task_1');

        expect(firstMember.parentId).toBe('graph_1-graph-frame');
        expect(secondMember.parentId).toBe('graph_1-graph-frame');

        // Saved positions are CONTENT-origin coordinates; the header band sits above them.
        expect(firstMember.position).toEqual({x: 0, y: GRAPH_FRAME_HEADER_HEIGHT});
        expect(secondMember.position).toEqual({x: 300, y: GRAPH_FRAME_HEADER_HEIGHT});

        // Members stay draggable inside the frame regardless of the global drag lock, and cannot
        // be dragged up into the header band.
        expect(firstMember.draggable).toBe(true);
        expect(firstMember.extent).toEqual([
            [0, GRAPH_FRAME_HEADER_HEIGHT],
            [Infinity, Infinity],
        ]);

        // Mirrors the leftmost PAINTED inset: the two members sit at x 0 and 300 and paint 72 wide, so
        // the block reaches 372 and the flush-left member gets the loop gutter mirrored back. Height
        // carries the header band, and stays under the configured minimum here.
        const frameNode = findNode(framed.outerNodes, 'graph_1-graph-frame');

        expect(frameNode.data.graphFrame).toMatchObject({graphId: 'graph_1', height: 200, width: 444});
        expect(frameNode.width).toBe(444);
        expect(frameNode.height).toBe(200);

        const outerNodeIds = framed.outerNodes.map((node) => node.id);

        expect(outerNodeIds).toEqual(['graph_1', 'graph_1-graph-frame']);

        // The Start pill and the add-node placeholder are frame chrome, so they ride along with
        // the members instead of being laid out by the outer engine.
        expect(framed.memberNodes.map((node) => node.id).sort()).toEqual([
            'graph_1-graph-placeholder',
            'graph_1-graph-start',
            'task_0',
            'task_1',
        ]);

        expect(framed.autoPlaced).toEqual({});
    });

    it('auto-places a member that has no saved position', async () => {
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [{name: 'task_0', position: {x: 0, y: 0}}, {name: 'task_1'}],
            [{from: 'task_0', to: 'task_1'}]
        );

        const framed = await layoutGraphFrames(nodes, edges, 'TB', stackedLayout);

        expect(Object.keys(framed.autoPlaced)).toEqual(['graph_1']);
        expect(Object.keys(framed.autoPlaced.graph_1)).toEqual(['task_1']);

        const autoPlacedPosition = framed.autoPlaced.graph_1.task_1;

        // Auto-placed members are offset below the pinned ones so the two groups never overlap.
        expect(autoPlacedPosition.y).toBeGreaterThan(0);

        expect(findNode(framed.memberNodes, 'task_1').position).toEqual({
            x: autoPlacedPosition.x,
            y: autoPlacedPosition.y + GRAPH_FRAME_HEADER_HEIGHT,
        });

        // The pinned member keeps its saved spot.
        expect(findNode(framed.memberNodes, 'task_0').position).toEqual({x: 0, y: GRAPH_FRAME_HEADER_HEIGHT});
    });

    it('clears the Start pill when every member has to be auto-placed', async () => {
        // The default rendering of a freshly-created graph: nothing has a stored position yet, so
        // every member is auto-placed. The pill is pinned at the content origin and is NOT a
        // member, so it only enters `autoPlaceGraphMembers`'s pinned set if the pre-pass puts it
        // there — and that set is what the auto-placed block is offset below by one frame padding.
        // Without it the block clears the pill by 4px, and only because ELK happens to pad its own
        // graph by 12: an accident, not a margin anyone chose.
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [{name: 'task_0'}, {name: 'task_1'}],
            [{from: 'task_0', to: 'task_1'}]
        );

        const framed = await layoutGraphFrames(nodes, edges, 'TB', stackedLayout);

        for (const memberName of ['task_0', 'task_1']) {
            const memberContentTop = fromFrameChildPosition(findNode(framed.memberNodes, memberName).position).y;

            expect(memberContentTop).toBeGreaterThanOrEqual(GRAPH_START_SIZE.height + GRAPH_FRAME_PADDING);
        }

        // The pill is chrome, centred on the cross axis at the head of the flow: in TB that is the
        // top of the content band, horizontally centred on the frame.
        // The pill marks where the graph is ENTERED, so it lines up with the entry member's own
        // painted box — which is what makes the start edge run straight into it.
        const startPosition = findNode(framed.memberNodes, 'graph_1-graph-start').position;
        const entryPosition = findNode(framed.memberNodes, 'task_0').position;

        expect(startPosition.y).toBe(GRAPH_FRAME_HEADER_HEIGHT);
        expect(startPosition.x + GRAPH_START_SIZE.width / 2).toBe(entryPosition.x + GRAPH_MEMBER_BOX_WIDTH / 2);
    });

    it('centres the Start pill down the left edge in LR', async () => {
        const {edges, nodes} = buildGraphFixture('graph_1', [{name: 'task_0'}], []);

        const framed = await layoutGraphFrames(nodes, edges, 'LR', stackedLayout);

        const startNode = findNode(framed.memberNodes, 'graph_1-graph-start');
        const entryNode = findNode(framed.memberNodes, 'task_0');

        // The pill centres on the member's PAINTED box, not the wider cross-axis footprint the
        // engine reserves for it — that is what puts it beside the box rather than below it.
        expect(startNode.position.x).toBe(GRAPH_FRAME_PADDING);
        expect(startNode.position.y + GRAPH_START_SIZE.height / 2).toBe(
            entryNode.position.y + GRAPH_MEMBER_BOX_WIDTH / 2
        );
    });

    it('moves transition and start edges onto the member edge list', async () => {
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [
                {name: 'task_0', position: {x: 0, y: 0}},
                {name: 'task_1', position: {x: 300, y: 0}},
            ],
            [{from: 'task_0', to: 'task_1'}]
        );

        const framed = await layoutGraphFrames(nodes, edges, 'TB', stackedLayout);

        expect(framed.memberEdges.map((edge) => edge.type).sort()).toEqual([
            GRAPH_START_EDGE_TYPE,
            GRAPH_TRANSITION_EDGE_TYPE,
        ]);

        expect(framed.outerEdges.some((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toBe(false);
        expect(framed.outerEdges.some((edge) => edge.type === GRAPH_START_EDGE_TYPE)).toBe(false);

        // The container chain the surrounding workflow addresses the box through survives.
        expect(framed.outerEdges.map((edge) => `${edge.source}=>${edge.target}`)).toEqual([
            'graph_1=>graph_1-graph-frame',
        ]);
    });

    it('drops an edge that would escape the frame', async () => {
        const {edges, nodes} = buildGraphFixture('graph_1', [{name: 'task_0', position: {x: 0, y: 0}}]);

        const escapingEdge: Edge = {id: 'task_0=>after', source: 'task_0', target: 'after', type: 'workflow'};

        const afterNode: Node = {
            data: {componentName: 'mailchimp'},
            id: 'after',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const framed = await layoutGraphFrames([...nodes, afterNode], [...edges, escapingEdge], 'TB', stackedLayout);

        expect(framed.outerEdges).not.toContain(escapingEdge);
        expect(framed.memberEdges).not.toContain(escapingEdge);
        expect(framed.outerNodes.map((node) => node.id)).toContain('after');
    });

    it('lays a dispatcher member out as one block and keeps its subtree inside the frame', async () => {
        const {edges, nodes} = buildGraphFixture('graph_1', [{name: 'loop_1', position: {x: 10, y: 20}}]);

        const loopMemberNode = findNode(nodes, 'loop_1');

        loopMemberNode.data = {...loopMemberNode.data, componentName: 'loop', taskDispatcherId: 'loop_1'};

        const loopChildNode: Node = {
            data: {componentName: 'mailchimp', loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner'},
            id: 'inner',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const framed = await layoutGraphFrames(
            [...nodes, loopChildNode],
            [...edges, {id: 'loop_1=>inner', source: 'loop_1', target: 'inner', type: 'workflow'}],
            'TB',
            stackedLayout
        );

        // The stub stacks the member and its child 100px apart; the member anchors the group's
        // bounding box, so it lands exactly on the saved position and the child follows below it.
        expect(findNode(framed.memberNodes, 'loop_1').position).toEqual({x: 10, y: 20 + GRAPH_FRAME_HEADER_HEIGHT});
        expect(findNode(framed.memberNodes, 'inner').position).toEqual({
            x: 10,
            y: 120 + GRAPH_FRAME_HEADER_HEIGHT,
        });

        expect(findNode(framed.memberNodes, 'inner').parentId).toBe('graph_1-graph-frame');

        // The intra-subtree edge is internal to the group, so it stays inside the frame.
        expect(framed.memberEdges.map((edge) => edge.id)).toContain('loop_1=>inner');

        // Frame height covers the two stacked boxes (200 tall) plus header and padding.
        expect(findNode(framed.outerNodes, 'graph_1-graph-frame').data.graphFrame).toMatchObject({
            graphId: 'graph_1',
            height: 284,
            width: 322,
        });
    });

    it('processes a nested graph first and treats its frame as a leaf of the outer member group', async () => {
        const outerDispatcherNode = createGraphDispatcherNode(
            'graph_1',
            [{name: 'graph_2', position: {x: 0, y: 0}}],
            []
        );

        const innerDispatcherNode = createGraphDispatcherNode(
            'graph_2',
            [{name: 'task_a', position: {x: 0, y: 0}}],
            [],
            {graphId: 'graph_1', index: 0}
        );

        const nodes: Node[] = [
            ...createGraphNode({allNodes: [outerDispatcherNode], graphId: 'graph_1'}),
            ...createGraphNode({allNodes: [innerDispatcherNode], graphId: 'graph_2', isNested: true}),
            createMemberTaskNode('graph_2', {name: 'task_a', position: {x: 0, y: 0}}, 0),
        ];

        const edges: Edge[] = [...createGraphEdges(outerDispatcherNode), ...createGraphEdges(innerDispatcherNode)];

        const framed = await layoutGraphFrames(nodes, edges, 'TB', stackedLayout);

        // Only the OUTER frame stays in the outer arrays; the inner one became a member of it.
        expect(framed.outerNodes.map((node) => node.id)).toEqual(['graph_1', 'graph_1-graph-frame']);

        expect(findNode(framed.memberNodes, 'graph_2-graph-frame').parentId).toBe('graph_1-graph-frame');
        expect(findNode(framed.memberNodes, 'task_a').parentId).toBe('graph_2-graph-frame');

        // React Flow requires a parent to precede its children in the node array.
        const memberNodeIds = framed.memberNodes.map((node) => node.id);

        expect(memberNodeIds.indexOf('graph_2-graph-frame')).toBeLessThan(memberNodeIds.indexOf('task_a'));

        // The inner frame was sized before the outer group measured it, so the outer frame is at
        // least as wide as the inner one plus padding.
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
    // The visual check Step 5 asked for — "the box renders identically under both engines" — as a
    // property that can actually be asserted: the frame's computed size and every member's
    // frame-relative position must not depend on which engine laid the member groups out.
    //
    // The `loop_1` member is what gives this test teeth. A single-node group is normalised onto its
    // own bounding-box minimum, so the engine's output cancels out by construction and the two
    // engines cannot disagree. A dispatcher member's group is FOUR nodes whose bounding box comes
    // from the engine's own placement, so `group.boundingBox` — and through it the frame's height —
    // is genuinely engine-derived here.
    const buildParityFixture = () => {
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [
                {name: 'task_0', position: {x: 0, y: 0}},
                {name: 'loop_1', position: {x: 300, y: 0}},
                {name: 'task_2', position: {x: 150, y: 320}},
            ],
            [
                {from: 'task_0', to: 'loop_1'},
                {condition: '${loop_1.ok}', from: 'loop_1', to: 'task_2'},
            ]
        );

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
        // `getElkLayoutElements` swallows any failure and returns the dagre result, and
        // `layoutGraphFrames` does not surface `engine` — so without this the assertions below
        // would stay green even if ELK never ran at all.
        const {edges, nodes} = buildParityFixture();

        const elkResult = await getElkLayoutElements({canvasWidth: 1200, direction: 'TB', edges, nodes});

        expect(elkResult.engine).toBe('elk');
    });

    it('puts every member at the same frame-relative position under dagre and ELK', async () => {
        const dagreFixture = buildParityFixture();
        const elkFixture = buildParityFixture();

        const dagreFramed = await layoutGraphFrames(dagreFixture.nodes, dagreFixture.edges, 'TB', getLayoutElements);
        const elkFramed = await layoutGraphFrames(elkFixture.nodes, elkFixture.edges, 'TB', getElkLayoutElements);

        for (const memberName of ['task_0', 'loop_1', 'task_2']) {
            expect(findNode(elkFramed.memberNodes, memberName).position).toEqual(
                findNode(dagreFramed.memberNodes, memberName).position
            );
        }
    });

    // The frame's SIZE is deliberately not asserted equal across engines. It hugs whatever the
    // active engine produced for a member's subtree, and the two engines have never spaced a
    // dispatcher's interior identically — this fixture's loop member measures 464px tall under
    // dagre and 408px under ELK. That difference is the engines' own rhythm showing through the
    // box, not a defect in the pre-pass; what must hold on both is that the box contains what it
    // was sized around.
    it.each([
        ['dagre', getLayoutElements],
        ['elk', getElkLayoutElements],
    ])('sizes the frame to contain every one of its children under %s', async (_engineName, layoutFunction) => {
        const {edges, nodes} = buildParityFixture();

        const framed = await layoutGraphFrames(nodes, edges, 'TB', layoutFunction);

        const frameSize = findNode(framed.outerNodes, 'graph_1-graph-frame').data.graphFrame as {
            height: number;
            width: number;
        };

        for (const memberNode of framed.memberNodes) {
            // The add-node placeholder is never painted and parks at the node default (0, 0),
            // above the content origin — it is an insertion anchor, not a laid-out child.
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
    /**
     * The read-only conversion runs BEFORE the frame pre-pass in `useLayout`, so what
     * `partitionFrameEdges` sees is already-rewritten edges. That ordering is exactly what used to
     * strip a read-only graph of its routing: a transition rewritten to `smoothstep` is neither a
     * transition nor a within-member edge, so the partition dropped it on the floor.
     */
    it('routes a read-only graph transition and start edge into the frame with their types intact', async () => {
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [{name: 'task_0'}, {name: 'task_1'}],
            [{from: 'task_0', to: 'task_1'}]
        );

        const framed = await layoutGraphFrames(nodes, toReadOnlyLayoutEdges(edges), 'TB', stackedLayout);

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
        const {edges, nodes} = buildGraphFixture(
            'graph_1',
            [{name: 'task_0'}, {name: 'task_1'}],
            [{from: 'task_0', to: 'task_1'}]
        );

        const framed = await layoutGraphFrames(nodes, toReadOnlyLayoutEdges(edges), 'TB', stackedLayout);

        // The pre-pass stamps members draggable regardless of read-only-ness, which is why the
        // canvas-wide `nodesDraggable={false}` is not enough on its own.
        expect(framed.memberNodes.filter((node) => node.draggable).length).toBeGreaterThan(0);

        expect(toReadOnlyLayoutNodes(framed.memberNodes).some((node) => node.draggable)).toBe(false);
    });
});
