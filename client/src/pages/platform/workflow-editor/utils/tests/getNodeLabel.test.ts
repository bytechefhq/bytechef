import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getNodeLabel} from '../getNodeLabel';

const makeWorkflow = (overrides: Partial<Workflow> = {}): Workflow => ({
    tasks: [],
    ...overrides,
});

describe('getNodeLabel', () => {
    it('should return label from workflow task when it exists', () => {
        const workflow = makeWorkflow({
            tasks: [{label: 'Fresh Label', name: 'http_1', type: 'http/v1/get'}],
        });

        const result = getNodeLabel({
            fallbackLabel: 'Stale Label',
            workflow,
            workflowNodeName: 'http_1',
        });

        expect(result).toBe('Fresh Label');
    });

    it('should return label from workflow trigger when it exists', () => {
        const workflow = makeWorkflow({
            triggers: [{label: 'Trigger Label', name: 'webhook_1', type: 'webhook/v1/newEvent'}],
        });

        const result = getNodeLabel({
            fallbackLabel: 'Old Trigger',
            workflow,
            workflowNodeName: 'webhook_1',
        });

        expect(result).toBe('Trigger Label');
    });

    it('should prefer trigger match over task match', () => {
        const workflow = makeWorkflow({
            tasks: [{label: 'Task Label', name: 'node_1', type: 'comp/v1/action'}],
            triggers: [{label: 'Trigger Label', name: 'node_1', type: 'comp/v1/trigger'}],
        });

        const result = getNodeLabel({
            fallbackLabel: 'Fallback',
            workflow,
            workflowNodeName: 'node_1',
        });

        expect(result).toBe('Trigger Label');
    });

    it('should fall back to fallbackLabel when node is not in workflow', () => {
        const workflow = makeWorkflow({tasks: []});

        const result = getNodeLabel({
            fallbackLabel: 'Fallback Label',
            workflow,
            workflowNodeName: 'missing_1',
        });

        expect(result).toBe('Fallback Label');
    });

    it('should fall back to fallbackLabel when task has no label', () => {
        const workflow = makeWorkflow({
            tasks: [{name: 'http_1', type: 'http/v1/get'}],
        });

        const result = getNodeLabel({
            fallbackLabel: 'Component Title',
            workflow,
            workflowNodeName: 'http_1',
        });

        expect(result).toBe('Component Title');
    });

    it('should return undefined when no label is found anywhere', () => {
        const workflow = makeWorkflow({tasks: []});

        const result = getNodeLabel({
            fallbackLabel: undefined,
            workflow,
            workflowNodeName: 'missing_1',
        });

        expect(result).toBeUndefined();
    });

    it('should find label in nested task dispatcher subtasks', () => {
        const workflow = makeWorkflow({
            tasks: [
                {
                    name: 'condition_1',
                    parameters: {
                        caseTrue: [{label: 'Nested Label', name: 'http_1', type: 'http/v1/get'}],
                    },
                    type: 'condition/v1',
                },
            ],
        });

        const result = getNodeLabel({
            fallbackLabel: 'Stale',
            workflow,
            workflowNodeName: 'http_1',
        });

        expect(result).toBe('Nested Label');
    });

    it('should handle undefined tasks gracefully', () => {
        const workflow = makeWorkflow({tasks: undefined});

        const result = getNodeLabel({
            fallbackLabel: 'Safe Fallback',
            workflow,
            workflowNodeName: 'http_1',
        });

        expect(result).toBe('Safe Fallback');
    });
});
