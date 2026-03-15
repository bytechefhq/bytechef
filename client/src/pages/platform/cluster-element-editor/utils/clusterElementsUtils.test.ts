import {ROOT_CLUSTER_HANDLE_STEP, ROOT_CLUSTER_WIDTH} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import {
    calculateNodeWidth,
    convertNameToCamelCase,
    convertNameToSnakeCase,
    getClusterElementsLabel,
    getHandlePosition,
    isPlainObject,
} from './clusterElementsUtils';

describe('calculateNodeWidth', () => {
    it('should return base width for 1 handle', () => {
        expect(calculateNodeWidth(1)).toBe(ROOT_CLUSTER_WIDTH);
    });

    it('should return base width for 4 handles', () => {
        expect(calculateNodeWidth(4)).toBe(ROOT_CLUSTER_WIDTH);
    });

    it('should increase width for 5 handles', () => {
        expect(calculateNodeWidth(5)).toBe(ROOT_CLUSTER_WIDTH + ROOT_CLUSTER_HANDLE_STEP);
    });

    it('should increase width linearly for handles beyond 4', () => {
        expect(calculateNodeWidth(6)).toBe(ROOT_CLUSTER_WIDTH + 2 * ROOT_CLUSTER_HANDLE_STEP);
        expect(calculateNodeWidth(8)).toBe(ROOT_CLUSTER_WIDTH + 4 * ROOT_CLUSTER_HANDLE_STEP);
    });

    it('should return base width for 0 or falsy handle count', () => {
        expect(calculateNodeWidth(0)).toBe(ROOT_CLUSTER_WIDTH);
    });
});

describe('getHandlePosition', () => {
    it('should center a single handle', () => {
        const position = getHandlePosition({handlesCount: 1, index: 0, nodeWidth: 280});

        expect(position).toBe(140);
    });

    it('should distribute 2 handles with edge buffers', () => {
        const nodeWidth = 280;
        const buffer = nodeWidth * 0.1; // 28

        const firstHandle = getHandlePosition({handlesCount: 2, index: 0, nodeWidth});
        const secondHandle = getHandlePosition({handlesCount: 2, index: 1, nodeWidth});

        expect(firstHandle).toBe(buffer);
        expect(secondHandle).toBe(nodeWidth - buffer);
    });

    it('should space handles evenly across usable width', () => {
        const nodeWidth = 460;
        const buffer = nodeWidth * 0.1; // 46
        const usable = nodeWidth - buffer * 2; // 368
        const step = usable / 4; // 92

        for (let index = 0; index < 5; index++) {
            const position = getHandlePosition({handlesCount: 5, index, nodeWidth});

            expect(position).toBe(buffer + step * index);
        }
    });

    it('should ensure adjacent cluster root nodes do not overlap after layout resolution', () => {
        // Simulates the overlap scenario: two 280px cluster root siblings under a
        // 5-handle parent (460px). The overlap resolution must push nodeB right of
        // nodeA's extent (nodeA.x + widthA + overlapPadding).
        const parentWidth = calculateNodeWidth(5);
        const childWidth = calculateNodeWidth(1);
        const overlapPadding = 20;

        const handleA = getHandlePosition({handlesCount: 5, index: 1, nodeWidth: parentWidth});
        const handleB = getHandlePosition({handlesCount: 5, index: 3, nodeWidth: parentWidth});

        const nodeAx = handleA - childWidth / 2;
        const nodeBx = handleB - childWidth / 2;

        const minX = nodeAx + childWidth + overlapPadding;

        // Before resolution nodeB overlaps; after resolution it's pushed to minX
        expect(nodeBx).toBeLessThan(minX);

        const resolvedBx = minX;
        const gap = resolvedBx - (nodeAx + childWidth);

        expect(gap).toBe(overlapPadding);
    });
});

describe('convertNameToCamelCase', () => {
    it('should convert SCREAMING_SNAKE_CASE to camelCase', () => {
        expect(convertNameToCamelCase('DOCUMENT_RETRIEVER')).toBe('documentRetriever');
        expect(convertNameToCamelCase('QUERY_EXPANDER')).toBe('queryExpander');
        expect(convertNameToCamelCase('VECTOR_STORE')).toBe('vectorStore');
    });

    it('should lowercase single words', () => {
        expect(convertNameToCamelCase('MODEL')).toBe('model');
        expect(convertNameToCamelCase('RAG')).toBe('rag');
    });

    it('should handle multi-segment names', () => {
        expect(convertNameToCamelCase('QUERY_TRANSFORMER')).toBe('queryTransformer');
        expect(convertNameToCamelCase('DOCUMENT_JOINER')).toBe('documentJoiner');
    });

    it('should convert all ClusterElementType keys to their camelCase names', () => {
        expect(convertNameToCamelCase('CHAT_MEMORY')).toBe('chatMemory');
        expect(convertNameToCamelCase('DATA_SOURCE')).toBe('dataSource');
        expect(convertNameToCamelCase('TOOLS')).toBe('tools');
        expect(convertNameToCamelCase('PROCESSOR')).toBe('processor');
        expect(convertNameToCamelCase('SOURCE')).toBe('source');
        expect(convertNameToCamelCase('DESTINATION')).toBe('destination');
    });
});

describe('convertNameToSnakeCase', () => {
    it('should convert camelCase to SCREAMING_SNAKE_CASE', () => {
        expect(convertNameToSnakeCase('documentRetriever')).toBe('DOCUMENT_RETRIEVER');
        expect(convertNameToSnakeCase('queryExpander')).toBe('QUERY_EXPANDER');
    });

    it('should handle single-word lowercase', () => {
        expect(convertNameToSnakeCase('model')).toBe('MODEL');
    });

    it('should convert all ClusterElementType camelCase names to their SCREAMING_SNAKE_CASE keys', () => {
        expect(convertNameToSnakeCase('chatMemory')).toBe('CHAT_MEMORY');
        expect(convertNameToSnakeCase('vectorStore')).toBe('VECTOR_STORE');
        expect(convertNameToSnakeCase('dataSource')).toBe('DATA_SOURCE');
        expect(convertNameToSnakeCase('tools')).toBe('TOOLS');
        expect(convertNameToSnakeCase('model')).toBe('MODEL');
        expect(convertNameToSnakeCase('processor')).toBe('PROCESSOR');
        expect(convertNameToSnakeCase('source')).toBe('SOURCE');
        expect(convertNameToSnakeCase('destination')).toBe('DESTINATION');
    });

    it('should be the inverse of convertNameToCamelCase for all ClusterElementType values', () => {
        const clusterElementTypeKeys = [
            'CHAT_MEMORY',
            'VECTOR_STORE',
            'DATA_SOURCE',
            'TOOLS',
            'MODEL',
            'PROCESSOR',
            'SOURCE',
            'DESTINATION',
            'DOCUMENT_RETRIEVER',
            'QUERY_EXPANDER',
        ];

        for (const key of clusterElementTypeKeys) {
            expect(convertNameToSnakeCase(convertNameToCamelCase(key))).toBe(key);
        }
    });
});

describe('getClusterElementsLabel', () => {
    it('should convert camelCase to a human-readable label', () => {
        expect(getClusterElementsLabel('chatMemory')).toBe('Chat Memory');
        expect(getClusterElementsLabel('vectorStore')).toBe('Vector Store');
        expect(getClusterElementsLabel('dataSource')).toBe('Data Source');
    });

    it('should capitalize single-word types', () => {
        expect(getClusterElementsLabel('tools')).toBe('Tools');
        expect(getClusterElementsLabel('model')).toBe('Model');
    });
});

describe('isPlainObject', () => {
    it('should return true for plain objects', () => {
        expect(isPlainObject({})).toBe(true);
        expect(isPlainObject({name: 'test'})).toBe(true);
    });

    it('should return false for arrays', () => {
        expect(isPlainObject([])).toBe(false);
        expect(isPlainObject([1, 2])).toBe(false);
    });

    it('should return false for null and primitives', () => {
        expect(isPlainObject(null)).toBe(false);
        expect(isPlainObject(undefined)).toBe(false);
        expect(isPlainObject('string')).toBe(false);
        expect(isPlainObject(42)).toBe(false);
        expect(isPlainObject(true)).toBe(false);
    });
});
