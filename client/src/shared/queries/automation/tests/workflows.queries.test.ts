import {describe, expect, it} from 'vitest';

import {WorkflowKeys} from '../workflows.queries';

describe('workflows.queries', () => {
    describe('WorkflowKeys', () => {
        it('generates correct key for workflows', () => {
            expect(WorkflowKeys.workflows).toEqual(['automationWorkflows']);
        });

        it('generates correct key for workflow by id', () => {
            const result = WorkflowKeys.workflow('workflow-123');

            expect(result).toEqual(['automationWorkflows', 'workflow-123']);
        });

        it('generates correct key for workflow by workflow execution id', () => {
            const result = WorkflowKeys.workflowByWorkflowExecutionId('execution-456');

            expect(result).toEqual(['automationWorkflows', 'execution', 'execution-456']);
        });

        it('generates unique keys for different workflow ids', () => {
            const key1 = WorkflowKeys.workflow('id-1');
            const key2 = WorkflowKeys.workflow('id-2');

            expect(key1).not.toEqual(key2);
            expect(key1).toEqual(['automationWorkflows', 'id-1']);
            expect(key2).toEqual(['automationWorkflows', 'id-2']);
        });

        it('generates unique keys for different execution ids', () => {
            const key1 = WorkflowKeys.workflowByWorkflowExecutionId('exec-1');
            const key2 = WorkflowKeys.workflowByWorkflowExecutionId('exec-2');

            expect(key1).not.toEqual(key2);
            expect(key1).toEqual(['automationWorkflows', 'execution', 'exec-1']);
            expect(key2).toEqual(['automationWorkflows', 'execution', 'exec-2']);
        });

        it('workflow key includes base workflows key', () => {
            const workflowKey = WorkflowKeys.workflow('test-id');

            expect(workflowKey[0]).toBe(WorkflowKeys.workflows[0]);
        });

        it('workflowByWorkflowExecutionId key includes base workflows key', () => {
            const executionKey = WorkflowKeys.workflowByWorkflowExecutionId('test-exec-id');

            expect(executionKey[0]).toBe(WorkflowKeys.workflows[0]);
        });
    });
});
