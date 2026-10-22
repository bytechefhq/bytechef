import {describe, expect, it} from 'vitest';

import resolveSyncedOperationName from '../resolveSyncedOperationName';

const currentNode = {operationName: 'appendValueToList', workflowNodeName: 'dataStorage_1'};

describe('resolveSyncedOperationName', () => {
    it('keeps the selected operation while an operation switch is saving', () => {
        expect(
            resolveSyncedOperationName({
                currentNode,
                isClusterElement: false,
                operationChangeInProgress: true,
                workflowNodes: [{operationName: 'atomicIncrement', workflowNodeName: 'dataStorage_1'}],
            })
        ).toBeUndefined();
    });

    it('takes the saved operation of the workflow node once no switch is in progress', () => {
        expect(
            resolveSyncedOperationName({
                currentNode,
                isClusterElement: false,
                operationChangeInProgress: false,
                workflowNodes: [
                    {operationName: 'setValue', workflowNodeName: 'dataStorage_reset'},
                    {operationName: 'atomicIncrement', workflowNodeName: 'dataStorage_1'},
                ],
            })
        ).toBe('atomicIncrement');
    });

    it('takes the saved operation of the cluster element', () => {
        expect(
            resolveSyncedOperationName({
                clusterElementComponentOperations: [{operationName: 'chat', workflowNodeName: 'dataStorage_1'}],
                currentNode,
                isClusterElement: true,
                operationChangeInProgress: false,
                workflowNodes: [{operationName: 'atomicIncrement', workflowNodeName: 'dataStorage_1'}],
            })
        ).toBe('chat');
    });

    it('falls back to the operation of the current node before the workflow has it', () => {
        expect(
            resolveSyncedOperationName({
                currentNode,
                isClusterElement: false,
                operationChangeInProgress: false,
                workflowNodes: [{operationName: 'setValue', workflowNodeName: 'dataStorage_reset'}],
            })
        ).toBe('appendValueToList');
    });

    it('resolves to an empty name when nothing names an operation', () => {
        expect(resolveSyncedOperationName({isClusterElement: false, operationChangeInProgress: false})).toBe('');
    });
});
