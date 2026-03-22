import {ON_ERROR_WIRE_KEY_ERROR_BRANCH, ON_ERROR_WIRE_KEY_MAIN_BRANCH} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createOnErrorEdges, {getOnErrorBranchSide, hasTaskInOnErrorBranches} from '../utils/createOnErrorEdges';

function makeOnErrorNode(onErrorId: string, mainBranch?: WorkflowTask[], errorBranch?: WorkflowTask[]): Node {
    return {
        data: {
            componentName: 'on-error',
            parameters: {
                ...(errorBranch !== undefined ? {[ON_ERROR_WIRE_KEY_ERROR_BRANCH]: errorBranch} : {}),
                ...(mainBranch !== undefined ? {[ON_ERROR_WIRE_KEY_MAIN_BRANCH]: mainBranch} : {}),
            },
        } as NodeDataType,
        id: onErrorId,
        position: {x: 0, y: 0},
    };
}

function makeTaskNode(taskId: string): Node {
    return {
        data: {componentName: taskId.split('_')[0]} as NodeDataType,
        id: taskId,
        position: {x: 0, y: 0},
    };
}

function findBranchStartIndices(
    edges: {source: string; target: string}[],
    onErrorId: string,
    leftTargetId: string,
    rightTargetId: string
): {leftIndex: number; rightIndex: number} {
    const topGhostId = `${onErrorId}-onError-top-ghost`;
    const topGhostOutEdges = edges.filter((edge) => edge.source === topGhostId);

    const leftIndex = topGhostOutEdges.findIndex((edge) => edge.target === leftTargetId);
    const rightIndex = topGhostOutEdges.findIndex((edge) => edge.target === rightTargetId);

    return {leftIndex, rightIndex};
}

describe('createOnErrorEdges', () => {
    describe('edge ordering for dagre left/right placement', () => {
        it('should insert left-branch content edges before right-branch placeholder edges', () => {
            const onErrorId = 'on-error_1';
            const mainTask = {name: 'mistral_1', type: 'mistral/v1'};

            const onErrorNode = makeOnErrorNode(onErrorId, [mainTask], undefined);
            const allNodes = [onErrorNode, makeTaskNode('mistral_1')];

            const edges = createOnErrorEdges(onErrorNode, allNodes);

            const rightPlaceholderId = `${onErrorId}-onError-right-placeholder-0`;
            const {leftIndex, rightIndex} = findBranchStartIndices(edges, onErrorId, 'mistral_1', rightPlaceholderId);

            expect(leftIndex).toBeGreaterThanOrEqual(0);
            expect(rightIndex).toBeGreaterThanOrEqual(0);
            expect(leftIndex).toBeLessThan(rightIndex);
        });
    });

    describe('edge structure', () => {
        it('should always start with on-error-to-top-ghost edge', () => {
            const onErrorId = 'on-error_1';
            const onErrorNode = makeOnErrorNode(onErrorId, undefined, undefined);

            const edges = createOnErrorEdges(onErrorNode, [onErrorNode]);

            expect(edges[0]).toMatchObject({
                source: onErrorId,
                target: `${onErrorId}-onError-top-ghost`,
            });
        });
    });
});

describe('getOnErrorBranchSide', () => {
    const tasks: WorkflowTask[] = [
        {
            name: 'on-error_1',
            parameters: {
                [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [{name: 'err_1', type: 'x/v1'}],
                [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [{name: 'main_1', type: 'y/v1'}],
            },
            type: 'on-error/v1',
        },
    ];

    it('should return left when task is in main-branch', () => {
        expect(getOnErrorBranchSide('main_1', tasks, 'on-error_1')).toBe('left');
    });

    it('should return right when task is in on-error-branch', () => {
        expect(getOnErrorBranchSide('err_1', tasks, 'on-error_1')).toBe('right');
    });
});

describe('hasTaskInOnErrorBranches', () => {
    it('should return true when task is in either branch', () => {
        const tasks: WorkflowTask[] = [
            {
                name: 'eh_1',
                parameters: {
                    [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [{name: 'b', type: 'x/v1'}],
                    [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [{name: 'a', type: 'y/v1'}],
                },
                type: 'on-error/v1',
            },
        ];

        expect(hasTaskInOnErrorBranches('eh_1', 'a', tasks)).toBe(true);
        expect(hasTaskInOnErrorBranches('eh_1', 'b', tasks)).toBe(true);
        expect(hasTaskInOnErrorBranches('eh_1', 'missing', tasks)).toBe(false);
    });
});
