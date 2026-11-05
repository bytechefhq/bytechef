import {Edge, Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    CLUSTER_FRAME_MEMBER_ROW_Y,
    CLUSTER_FRAME_NESTED_ROOT_EXTRA_HEIGHT,
    CLUSTER_FRAME_PLACEHOLDER_Y,
} from './clusterFrameGeometry';
import {placeClusterMembers} from './placeClusterMembers';

const hoisted = vi.hoisted(() => ({
    getClusterElementsLayoutElements: vi.fn(),
}));

vi.mock('../layoutUtils', () => ({
    getClusterElementsLayoutElements: hoisted.getClusterElementsLayoutElements,
}));

const ROOT_ID = 'aiAgent_1';

function buildMemberNode(id: string, overrides: Partial<Node> = {}): Node {
    return {
        data: {},
        id,
        parentId: ROOT_ID,
        position: {x: 5, y: 5},
        type: 'workflow',
        ...overrides,
    };
}

describe('placeClusterMembers', () => {
    beforeEach(() => {
        hoisted.getClusterElementsLayoutElements.mockReset();
    });

    it('returns the same empty list without laying anything out', () => {
        const memberNodes: Node[] = [];

        const result = placeClusterMembers({clusterRootId: ROOT_ID, edges: [], memberNodes, rootClusterElements: {}});

        expect(result).toBe(memberNodes);
        expect(hoisted.getClusterElementsLayoutElements).not.toHaveBeenCalled();
    });

    it('lays out members around a synthetic root on the member row', () => {
        const edges: Edge[] = [{id: 'edge_1', source: ROOT_ID, target: 'model_1'}];
        const modelNode = buildMemberNode('model_1', {data: {parentClusterRootElementsTypeCount: 3}});
        const rootClusterElements = {model: {name: 'model_1', type: 'openAi/v1/model'}};

        hoisted.getClusterElementsLayoutElements.mockReturnValue({edges, nodes: []});

        placeClusterMembers({clusterRootId: ROOT_ID, edges, memberNodes: [modelNode], rootClusterElements});

        expect(hoisted.getClusterElementsLayoutElements).toHaveBeenCalledWith({
            canvasHeight: 0,
            canvasWidth: 0,
            childRowY: CLUSTER_FRAME_MEMBER_ROW_Y,
            currentRootPosition: {x: 0, y: 0},
            edges,
            nodes: [
                {
                    data: {clusterElementTypesCount: 3, clusterElements: rootClusterElements},
                    id: ROOT_ID,
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
                modelNode,
            ],
        });
    });

    it('defaults the synthetic root to one element type and empty cluster elements', () => {
        const nestedNode = buildMemberNode('tool_1', {parentId: 'other_root'});

        hoisted.getClusterElementsLayoutElements.mockReturnValue({edges: [], nodes: []});

        placeClusterMembers({
            clusterRootId: ROOT_ID,
            edges: [],
            memberNodes: [nestedNode],
            rootClusterElements: undefined,
        });

        const [{nodes}] = hoisted.getClusterElementsLayoutElements.mock.calls[0];

        expect(nodes[0].data).toEqual({clusterElementTypesCount: 1, clusterElements: {}});
    });

    it('applies laid-out positions and keeps members the layout did not place', () => {
        const placedNode = buildMemberNode('model_1');
        const unplacedNode = buildMemberNode('memory_1');

        hoisted.getClusterElementsLayoutElements.mockReturnValue({
            edges: [],
            nodes: [{id: 'model_1', position: {x: 100, y: 200}}],
        });

        const result = placeClusterMembers({
            clusterRootId: ROOT_ID,
            edges: [],
            memberNodes: [placedNode, unplacedNode],
            rootClusterElements: {},
        });

        expect(result[0]).toEqual({...placedNode, position: {x: 100, y: 200}});
        expect(result[1]).toBe(unplacedNode);
    });

    it('places direct placeholders on the placeholder row and nested ones lower, keeping their x', () => {
        const directPlaceholder = buildMemberNode('direct-placeholder', {position: {x: 40, y: 0}, type: 'placeholder'});
        const nestedPlaceholder = buildMemberNode('nested-placeholder', {
            parentId: 'tool_1',
            position: {x: 60, y: 0},
            type: 'placeholder',
        });

        hoisted.getClusterElementsLayoutElements.mockReturnValue({
            edges: [],
            nodes: [{id: 'direct-placeholder', position: {x: 999, y: 999}}],
        });

        const result = placeClusterMembers({
            clusterRootId: ROOT_ID,
            edges: [],
            memberNodes: [directPlaceholder, nestedPlaceholder],
            rootClusterElements: {},
        });

        expect(result[0].position).toEqual({x: 40, y: CLUSTER_FRAME_PLACEHOLDER_Y});
        expect(result[1].position).toEqual({
            x: 60,
            y: CLUSTER_FRAME_PLACEHOLDER_Y + CLUSTER_FRAME_NESTED_ROOT_EXTRA_HEIGHT,
        });
    });
});
