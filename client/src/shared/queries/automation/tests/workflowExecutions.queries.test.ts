import {describe, expect, it} from 'vitest';

import {WorkflowExecutionKeys} from '../workflowExecutions.queries';

describe('workflowExecutions.queries', () => {
    describe('WorkflowExecutionKeys', () => {
        it('generates correct base key for workflow executions', () => {
            expect(WorkflowExecutionKeys.workflowExecutions).toEqual(['automation_workflowExecutions']);
        });

        it('generates correct key for workflow execution by id', () => {
            const result = WorkflowExecutionKeys.workflowExecution(123);

            expect(result).toEqual(['automation_workflowExecutions', 123]);
        });

        it('generates correct key for filtered workflow executions', () => {
            const request = {
                id: 1,
                pageNumber: 0,
                pageSize: 10,
                projectId: 1,
            };

            const result = WorkflowExecutionKeys.filteredWorkflowExecutions(request);

            expect(result).toEqual(['automation_workflowExecutions', request]);
        });

        it('generates unique keys for different workflow execution ids', () => {
            const key1 = WorkflowExecutionKeys.workflowExecution(1);
            const key2 = WorkflowExecutionKeys.workflowExecution(2);

            expect(key1).not.toEqual(key2);
            expect(key1).toEqual(['automation_workflowExecutions', 1]);
            expect(key2).toEqual(['automation_workflowExecutions', 2]);
        });

        it('generates unique keys for different filter requests', () => {
            const request1 = {id: 1, pageNumber: 0, projectId: 1};
            const request2 = {id: 1, pageNumber: 0, projectId: 2};

            const key1 = WorkflowExecutionKeys.filteredWorkflowExecutions(request1);
            const key2 = WorkflowExecutionKeys.filteredWorkflowExecutions(request2);

            expect(key1).not.toEqual(key2);
        });

        it('workflow execution key includes base workflow executions key', () => {
            const executionKey = WorkflowExecutionKeys.workflowExecution(123);

            expect(executionKey[0]).toBe(WorkflowExecutionKeys.workflowExecutions[0]);
        });

        it('filtered workflow executions key includes base workflow executions key', () => {
            const filteredKey = WorkflowExecutionKeys.filteredWorkflowExecutions({id: 1, pageNumber: 0});

            expect(filteredKey[0]).toBe(WorkflowExecutionKeys.workflowExecutions[0]);
        });
    });
});
