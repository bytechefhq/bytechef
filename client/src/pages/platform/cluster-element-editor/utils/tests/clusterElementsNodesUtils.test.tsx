import {DEFAULT_NODE_POSITION} from '@/shared/constants';
import {ClusterElementItemType} from '@/shared/types';
import {ReactElement} from 'react';
import {describe, expect, it} from 'vitest';

import {
    createMultipleElementsNode,
    createPlaceholderNode,
    createSingleElementsNode,
} from '../clusterElementsNodesUtils';

const ROOT_ID = 'aiAgent_1';

function buildClusterElementItem(overrides: Partial<ClusterElementItemType> = {}): ClusterElementItemType {
    return {
        label: 'OpenAI Model',
        name: 'openAi_1',
        parameters: {model: 'gpt-4o'},
        type: 'openAi/v2/model',
        ...overrides,
    };
}

describe('createPlaceholderNode', () => {
    it('centres a single placeholder under a single-handle root', () => {
        const node = createPlaceholderNode({
            clusterElementTypeLabel: 'Model',
            clusterElementTypeName: 'model',
            clusterRootId: ROOT_ID,
        });

        expect(node).toEqual({
            data: {
                clusterElementLabel: 'Model',
                clusterElementRequired: false,
                clusterElementType: 'model',
                label: '+',
                multipleClusterElementsNode: false,
            },
            id: 'aiAgent_1-model-placeholder-0',
            parentId: ROOT_ID,
            position: {x: 128, y: 140},
            type: 'placeholder',
        });
    });

    it('aligns the placeholder with its handle on a multi-handle root', () => {
        const node = createPlaceholderNode({
            clusterElementTypeIndex: 2,
            clusterElementTypeLabel: 'Tools',
            clusterElementTypeName: 'tools',
            clusterRootId: ROOT_ID,
            isMultipleClusterElementsNode: true,
            parentClusterRootElementsTypeCount: 3,
        });

        expect(node.position).toEqual({x: 240, y: 140});
        expect(node.data.multipleClusterElementsNode).toBe(true);
    });

    it('places the first handle of a multi-handle root at the edge buffer', () => {
        const node = createPlaceholderNode({
            clusterElementTypeIndex: 0,
            clusterElementTypeLabel: 'Model',
            clusterElementTypeName: 'model',
            clusterRootId: ROOT_ID,
            parentClusterRootElementsTypeCount: 3,
        });

        expect(node.position).toEqual({x: 16, y: 140});
    });

    it('flags the placeholder of a required cluster element slot', () => {
        const node = createPlaceholderNode({
            clusterElementTypeLabel: 'Document Retriever',
            clusterElementTypeName: 'documentRetriever',
            clusterElementTypeRequired: true,
            clusterRootId: ROOT_ID,
        });

        expect(node.data.clusterElementRequired).toBe(true);
    });
});

describe('createSingleElementsNode', () => {
    it('builds a workflow node from the cluster element type segments', () => {
        const clusterElementItem = buildClusterElementItem({
            metadata: {ui: {nodePosition: {x: 30, y: 40}, placeholderPositions: {tools: {x: 1, y: 2}}}},
        });

        const node = createSingleElementsNode({
            clusterElementItem,
            clusterElementTypeIndex: 1,
            clusterElementTypeLabel: 'Model',
            clusterElementTypeName: 'model',
            clusterRootId: ROOT_ID,
            currentNestedRootElementTypesCount: 2,
            parentClusterRootElementsTypeCount: 4,
        });

        expect(node).toMatchObject({
            data: {
                clusterElementLabel: 'Model',
                clusterElementName: 'model',
                clusterElementType: 'model',
                clusterElementTypeIndex: 1,
                clusterElementTypesCount: 2,
                componentName: 'openAi',
                connections: [],
                label: 'OpenAI Model',
                metadata: {ui: {nodePosition: {x: 30, y: 40}, placeholderPositions: {tools: {x: 1, y: 2}}}},
                name: 'openAi_1',
                operationName: 'model',
                parameters: {model: 'gpt-4o'},
                parentClusterRootElementsTypeCount: 4,
                type: 'openAi/v2/model',
                version: 2,
                workflowNodeName: 'openAi_1',
            },
            id: 'openAi_1',
            parentId: ROOT_ID,
            position: {x: 30, y: 40},
            type: 'workflow',
        });
        expect(node.data.multipleClusterElementsNode).toBeUndefined();
    });

    it('points the icon at the component svg', () => {
        const node = createSingleElementsNode({
            clusterElementItem: buildClusterElementItem(),
            clusterElementTypeIndex: 0,
            clusterElementTypeLabel: 'Model',
            clusterElementTypeName: 'model',
            clusterRootId: ROOT_ID,
            parentClusterRootElementsTypeCount: 1,
        });

        expect((node.data.icon as ReactElement<{src: string}>).props.src).toBe('/icons/openAi.svg');
    });

    it('falls back to the default position and an empty ui block without metadata', () => {
        const node = createSingleElementsNode({
            clusterElementItem: buildClusterElementItem(),
            clusterElementTypeIndex: 0,
            clusterElementTypeLabel: 'Model',
            clusterElementTypeName: 'model',
            clusterRootId: ROOT_ID,
            parentClusterRootElementsTypeCount: 1,
        });

        expect(node.position).toEqual(DEFAULT_NODE_POSITION);
        expect(node.data.metadata).toEqual({ui: {nodePosition: undefined}});
    });
});

describe('createMultipleElementsNode', () => {
    it('builds a workflow node flagged as a multiple element', () => {
        const node = createMultipleElementsNode({
            clusterElementTypeIndex: 2,
            clusterElementTypeName: 'tools',
            clusterRootId: ROOT_ID,
            element: buildClusterElementItem({
                metadata: {ui: {nodePosition: {x: 7, y: 8}}},
                name: 'slack_1',
                type: 'slack/v1/sendMessage',
            }),
            isMultipleClusterElementsNode: true,
            parentClusterRootElementsTypeCount: 3,
        });

        expect(node).toMatchObject({
            data: {
                clusterElementName: 'sendMessage',
                clusterElementType: 'tools',
                clusterElementTypeIndex: 2,
                componentName: 'slack',
                multipleClusterElementsNode: true,
                operationName: 'sendMessage',
                parentClusterRootElementsTypeCount: 3,
                version: 1,
                workflowNodeName: 'slack_1',
            },
            id: 'slack_1',
            parentId: ROOT_ID,
            position: {x: 7, y: 8},
            type: 'workflow',
        });
        expect(node.data.clusterElementLabel).toBeUndefined();
        expect((node.data.icon as ReactElement<{src: string}>).props.src).toBe('/icons/slack.svg');
    });

    it('falls back to the default position without a stored one', () => {
        const node = createMultipleElementsNode({
            clusterElementTypeIndex: 0,
            clusterElementTypeName: 'tools',
            clusterRootId: ROOT_ID,
            element: buildClusterElementItem({metadata: {ui: {}}}),
            isMultipleClusterElementsNode: false,
            parentClusterRootElementsTypeCount: 1,
        });

        expect(node.position).toEqual(DEFAULT_NODE_POSITION);
        expect(node.data.multipleClusterElementsNode).toBe(false);
    });
});
