import {calculateNodeWidth} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {CLUSTER_ELEMENT_NODE_WIDTH, CLUSTER_ROOT_NODE_WIDTH} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    CLUSTER_FRAME_BOTTOM_PADDING,
    CLUSTER_FRAME_CONTENT_TOP,
    CLUSTER_FRAME_HEADER_HEIGHT,
    CLUSTER_FRAME_LABEL_OVERHANG,
    CLUSTER_FRAME_MEMBER_ROW_Y,
    CLUSTER_FRAME_MIN_HEIGHT,
    CLUSTER_FRAME_MIN_WIDTH,
    CLUSTER_FRAME_PADDING,
    CLUSTER_FRAME_PLACEHOLDER_Y,
    CLUSTER_FRAME_TITLE_LINE_HEIGHT,
    fromClusterFrameChildPosition,
    getClusterMemberSize,
    toClusterFrameChildPosition,
} from './clusterFrameGeometry';
import {layoutClusterFrames} from './layoutClusterFrames';

const ROOT_ID = 'aiAgent_1';

function buildRootNode(): Node {
    return {
        data: {clusterRoot: true, workflowNodeName: ROOT_ID},
        id: ROOT_ID,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

function buildElementNode(id: string, position: {x: number; y: number}, parentId: string = ROOT_ID): Node {
    return {
        data: {
            clusterElementType: 'model',
            metadata: {ui: {nodePosition: position}},
            parentClusterRootElementsTypeCount: 1,
            parentClusterRootId: parentId,
            workflowNodeName: id,
        },
        id,
        parentId,
        position,
        type: 'workflow',
    };
}

function buildPlaceholderElementNode(id: string, position: {x: number; y: number}): Node {
    return {
        data: {clusterElementType: 'model', label: '+'},
        id,
        parentId: ROOT_ID,
        position,
        type: 'placeholder',
    };
}

const ELEMENT_SIZE = getClusterMemberSize(buildElementNode('sizing_probe', {x: 0, y: 0}));

describe('layoutClusterFrames', () => {
    it('sizes the root from its elements and moves them out of the outer array', () => {
        const elementNode = buildElementNode('model_1', {x: 400, y: 300});

        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [elementNode]},
            },
            {}
        );

        const rootNode = result.outerNodes.find((node) => node.id === ROOT_ID)!;

        expect(rootNode.data.clusterFrame).toEqual({
            clusterRootId: ROOT_ID,
            contentOrigin: {x: CLUSTER_FRAME_PADDING, y: CLUSTER_FRAME_CONTENT_TOP},
            height: CLUSTER_FRAME_CONTENT_TOP + 300 + ELEMENT_SIZE.height + CLUSTER_FRAME_BOTTOM_PADDING,
            width: CLUSTER_FRAME_PADDING + 400 + ELEMENT_SIZE.width + CLUSTER_FRAME_PADDING,
        });

        expect(result.outerNodes.map((node) => node.id)).toEqual([ROOT_ID]);
        expect(result.memberNodes.map((node) => node.id)).toEqual(['model_1']);
    });

    it('floors the box at its minimum size and centres the contents in the slack', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: 0, y: 0})]},
            },
            {}
        );

        const contentWidth = Math.max(calculateNodeWidth(0), ELEMENT_SIZE.width);

        expect(result.outerNodes[0].data.clusterFrame).toEqual({
            clusterRootId: ROOT_ID,
            contentOrigin: {x: (CLUSTER_FRAME_MIN_WIDTH - contentWidth) / 2, y: CLUSTER_FRAME_CONTENT_TOP},
            height: CLUSTER_FRAME_MIN_HEIGHT,
            width: CLUSTER_FRAME_MIN_WIDTH,
        });
    });

    it('reserves room for the root card', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: 0, y: 600})]},
            },
            {}
        );

        const clusterFrame = result.outerNodes[0].data.clusterFrame as {height: number; width: number};

        expect(clusterFrame.width).toBeGreaterThanOrEqual(CLUSTER_ROOT_NODE_WIDTH + CLUSTER_FRAME_PADDING);
        expect(clusterFrame.height).toBeGreaterThanOrEqual(
            600 + ELEMENT_SIZE.height + CLUSTER_FRAME_CONTENT_TOP + CLUSTER_FRAME_BOTTOM_PADDING
        );
    });

    it('reserves the card at the width its element types give it', () => {
        const typeNames = ['model', 'memory', 'rag', 'guardrails', 'tools'];
        const placeholderNodes: Node[] = typeNames.map((typeName, index) => ({
            data: {clusterElementType: typeName, label: '+'},
            id: `${ROOT_ID}-${typeName}-placeholder-0`,
            parentId: ROOT_ID,
            position: {x: 40 * index, y: 140},
            type: 'placeholder',
        }));

        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: placeholderNodes},
            },
            {}
        );

        const clusterFrame = result.outerNodes[0].data.clusterFrame as {width: number};

        expect(clusterFrame.width).toBe(calculateNodeWidth(typeNames.length) + 2 * CLUSTER_FRAME_PADDING);
    });

    it('pushes the content origin in so a member at a negative x still lands inside the box', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: -120, y: 200})]},
            },
            {}
        );

        const clusterFrame = result.outerNodes[0].data.clusterFrame as {
            contentOrigin: {x: number; y: number};
            height: number;
            width: number;
        };

        expect(clusterFrame.contentOrigin.x).toBe(CLUSTER_FRAME_PADDING + 120);
        expect(result.memberNodes[0].position.x).toBe(CLUSTER_FRAME_PADDING);
        expect(result.memberNodes[0].position.x + ELEMENT_SIZE.width).toBeLessThanOrEqual(clusterFrame.width);

        expect(fromClusterFrameChildPosition(result.memberNodes[0].position, clusterFrame.contentOrigin)).toEqual({
            x: -120,
            y: 200,
        });
    });

    it('places unsaved members and placeholders one main-canvas edge below the root', () => {
        const unsavedElementNode: Node = {
            data: {
                clusterElementType: 'model',
                clusterElementTypeIndex: 0,
                parentClusterRootElementsTypeCount: 1,
                parentClusterRootId: ROOT_ID,
                workflowNodeName: 'model_1',
            },
            id: 'model_1',
            parentId: ROOT_ID,
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {
                    [ROOT_ID]: [
                        unsavedElementNode,
                        buildPlaceholderElementNode(`${ROOT_ID}-model-placeholder-0`, {x: 0, y: 140}),
                    ],
                },
            },
            {}
        );

        const clusterFrame = result.outerNodes[0].data.clusterFrame as {contentOrigin: {x: number; y: number}};

        const contentYById = new Map(
            result.memberNodes.map((memberNode) => [
                memberNode.id,
                fromClusterFrameChildPosition(memberNode.position, clusterFrame.contentOrigin).y,
            ])
        );

        expect(contentYById.get('model_1')).toBe(CLUSTER_FRAME_MEMBER_ROW_Y);
        expect(contentYById.get(`${ROOT_ID}-model-placeholder-0`)).toBe(CLUSTER_FRAME_PLACEHOLDER_Y);
    });

    it('leaves as much space below the last element label as the header leaves below its title', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: 0, y: 300})]},
            },
            {}
        );

        const clusterFrame = result.outerNodes[0].data.clusterFrame as {
            contentOrigin: {x: number; y: number};
            height: number;
        };

        const labelBottom =
            clusterFrame.contentOrigin.y + 300 + CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_FRAME_LABEL_OVERHANG;

        expect(clusterFrame.height - labelBottom).toBe(
            (CLUSTER_FRAME_HEADER_HEIGHT - CLUSTER_FRAME_TITLE_LINE_HEIGHT) / 2
        );
    });

    it('keeps nested members parented to their nested root and out of frame coordinates', () => {
        const nestedRootNode = buildElementNode('sub_agent_1', {x: 100, y: 200});
        const nestedMemberNode = buildElementNode('nested_model_1', {x: 10, y: 150}, 'sub_agent_1');

        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [nestedRootNode, nestedMemberNode]},
            },
            {}
        );

        const placedNestedRoot = result.memberNodes.find((node) => node.id === 'sub_agent_1')!;
        const placedNestedMember = result.memberNodes.find((node) => node.id === 'nested_model_1')!;
        const clusterFrame = result.outerNodes[0].data.clusterFrame as {contentOrigin: {x: number; y: number}};

        expect(placedNestedRoot.parentId).toBe(ROOT_ID);
        expect(placedNestedRoot.position).toEqual(
            toClusterFrameChildPosition({x: 100, y: 200}, clusterFrame.contentOrigin)
        );

        expect(placedNestedMember.parentId).toBe('sub_agent_1');
        expect(placedNestedMember.position).toEqual({x: 10, y: 150});
    });

    it('keeps only direct members inside the frame, leaving nested members free of the frame extent', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {
                    [ROOT_ID]: [
                        buildElementNode('sub_agent_1', {x: 100, y: 200}),
                        buildElementNode('nested_model_1', {x: -8, y: 150}, 'sub_agent_1'),
                    ],
                },
            },
            {}
        );

        const placedNestedRoot = result.memberNodes.find((node) => node.id === 'sub_agent_1')!;
        const placedNestedMember = result.memberNodes.find((node) => node.id === 'nested_model_1')!;

        expect(placedNestedRoot.extent).toEqual([
            [0, CLUSTER_FRAME_HEADER_HEIGHT],
            [Infinity, Infinity],
        ]);
        expect(placedNestedMember.extent).toBeUndefined();
    });

    it('sizes the box around a nested member', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {
                    [ROOT_ID]: [
                        buildElementNode('sub_agent_1', {x: 500, y: 200}),
                        buildElementNode('nested_model_1', {x: 300, y: 150}, 'sub_agent_1'),
                    ],
                },
            },
            {}
        );

        const clusterFrame = result.outerNodes[0].data.clusterFrame as {height: number; width: number};

        expect(clusterFrame.width).toBeGreaterThanOrEqual(500 + 300 + ELEMENT_SIZE.width);
        expect(clusterFrame.height).toBeGreaterThanOrEqual(200 + 150 + ELEMENT_SIZE.height);
    });

    it('leaves a root with no elements untouched', () => {
        const result = layoutClusterFrames([buildRootNode()], [], {edgesByRootId: {}, nodesByRootId: {}}, {});

        expect(result.outerNodes[0].data.clusterFrame).toBeUndefined();
        expect(result.memberNodes).toEqual([]);
    });

    it('keeps a cluster root inside a graph member out of the outer array', () => {
        const rootInGraph: Node = {
            data: {clusterRoot: true, graphData: {graphId: 'graph_1', index: 0}, workflowNodeName: ROOT_ID},
            id: ROOT_ID,
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const result = layoutClusterFrames(
            [rootInGraph],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: 400, y: 300})]},
            },
            {}
        );

        expect(result.outerNodes.map((node) => node.id)).toEqual([ROOT_ID]);
        expect(result.memberNodes.map((node) => node.id)).toEqual(['model_1']);
        expect((result.outerNodes[0].data.clusterFrame as {width: number}).width).toBeGreaterThan(
            CLUSTER_FRAME_MIN_WIDTH
        );
    });

    it('partitions edges between the box and the outer array in both directions', () => {
        const memberEdge: Edge = {id: 'edge_root_model', source: ROOT_ID, target: 'model_1'};
        const edgeTouchingMember: Edge = {id: 'edge_model_other', source: 'model_1', target: 'other_node'};
        const chainEdge: Edge = {id: 'edge_chain', source: 'task_1', target: 'task_2'};

        const result = layoutClusterFrames(
            [buildRootNode()],
            [edgeTouchingMember, chainEdge],
            {
                edgesByRootId: {[ROOT_ID]: [memberEdge]},
                nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: 0, y: 0})]},
            },
            {}
        );

        expect(result.memberEdges.map((edge) => edge.id)).toEqual(['edge_root_model']);

        expect(result.outerEdges.map((edge) => edge.id)).toEqual(['edge_chain']);
    });

    it('marks elements draggable only when their root is unlocked', () => {
        const elements = {
            edgesByRootId: {[ROOT_ID]: []},
            nodesByRootId: {[ROOT_ID]: [buildElementNode('model_1', {x: 0, y: 0})]},
        };

        expect(layoutClusterFrames([buildRootNode()], [], elements, {}).memberNodes[0].draggable).toBe(false);

        expect(layoutClusterFrames([buildRootNode()], [], elements, {[ROOT_ID]: false}).memberNodes[0].draggable).toBe(
            true
        );
    });

    it('never marks a placeholder element draggable, even on an unlocked root', () => {
        const result = layoutClusterFrames(
            [buildRootNode()],
            [],
            {
                edgesByRootId: {[ROOT_ID]: []},
                nodesByRootId: {
                    [ROOT_ID]: [
                        buildElementNode('model_1', {x: 0, y: 0}),
                        buildPlaceholderElementNode('model-placeholder-0', {x: 260, y: 0}),
                    ],
                },
            },
            {[ROOT_ID]: false}
        );

        const placeholderNode = result.memberNodes.find((node) => node.id === 'model-placeholder-0')!;
        const elementNode = result.memberNodes.find((node) => node.id === 'model_1')!;

        expect(placeholderNode.draggable).toBe(false);
        expect(elementNode.draggable).toBe(true);
    });
});

describe('getClusterMemberSize', () => {
    it('sizes an unmeasured member from its kind rather than to zero', () => {
        const size = getClusterMemberSize(buildElementNode('model_1', {x: 0, y: 0}));

        expect(size.height).toBeGreaterThan(0);
        expect(size.width).toBeGreaterThan(0);
    });

    it('sizes a nested cluster root wider than a plain element, and a placeholder smaller', () => {
        const nestedClusterRootNode = buildElementNode('sub_agent_1', {x: 0, y: 0});

        nestedClusterRootNode.data.clusterElementTypesCount = 2;

        expect(getClusterMemberSize(nestedClusterRootNode).width).toBeGreaterThan(ELEMENT_SIZE.width);
        expect(getClusterMemberSize(buildPlaceholderElementNode('p', {x: 0, y: 0})).width).toBeLessThan(
            ELEMENT_SIZE.width
        );
    });

    it('falls back to the measured size for a shape it does not recognise', () => {
        const unknownNode: Node = {
            data: {},
            id: 'unknown',
            measured: {height: 55, width: 155},
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        expect(getClusterMemberSize(unknownNode)).toEqual({height: 55, width: 155});
    });
});

describe('toClusterFrameChildPosition / fromClusterFrameChildPosition', () => {
    it('are inverses of each other', () => {
        const contentPosition = {x: 123, y: 456};

        expect(fromClusterFrameChildPosition(toClusterFrameChildPosition(contentPosition))).toEqual(contentPosition);
    });

    it('are inverses of each other under a shifted content origin', () => {
        const contentPosition = {x: -120, y: 456};
        const contentOrigin = {x: 120, y: CLUSTER_FRAME_HEADER_HEIGHT};

        expect(
            fromClusterFrameChildPosition(toClusterFrameChildPosition(contentPosition, contentOrigin), contentOrigin)
        ).toEqual(contentPosition);
    });
});
