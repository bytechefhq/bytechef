import {NodeDataType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import getNodeOperationDescription from './getNodeOperationDescription';

describe('getNodeOperationDescription', () => {
    it('returns the trigger description for trigger nodes', () => {
        const description = getNodeOperationDescription({
            actionDescription: 'action description',
            currentNode: {trigger: true, workflowNodeName: 'trigger_1'} as NodeDataType,
            currentOperationName: 'newEmail',
            triggerDescription: 'trigger description',
        });

        expect(description).toBe('trigger description');
    });

    it('returns the action description for regular action nodes', () => {
        const description = getNodeOperationDescription({
            actionDescription: 'action description',
            currentNode: {workflowNodeName: 'gmail_1'} as NodeDataType,
            currentOperationName: 'sendEmail',
        });

        expect(description).toBe('action description');
    });

    it('returns the selected operation description for a non-root cluster element with multiple actions', () => {
        const description = getNodeOperationDescription({
            actionDescription: 'fallback action description',
            clusterElementOperations: [
                {description: 'Reads a document.', name: 'read'},
                {description: 'Writes a document.', name: 'write'},
            ],
            currentNode: {
                clusterElementType: 'DOCUMENT_READER',
                workflowNodeName: 'documentReader_1',
            } as NodeDataType,
            currentOperationName: 'write',
            rootClusterElementWorkflowNodeName: 'knowledgeBase_1',
        });

        // Regression guard for #5122: must be the selected action description, not the component description.
        expect(description).toBe('Writes a document.');
    });

    it('falls back to the action description for the root cluster element node', () => {
        const description = getNodeOperationDescription({
            actionDescription: 'root component description',
            clusterElementOperations: [{description: 'Reads a document.', name: 'read'}],
            currentNode: {
                clusterElementType: 'DOCUMENT_READER',
                workflowNodeName: 'knowledgeBase_1',
            } as NodeDataType,
            currentOperationName: 'read',
            rootClusterElementWorkflowNodeName: 'knowledgeBase_1',
        });

        expect(description).toBe('root component description');
    });
});
