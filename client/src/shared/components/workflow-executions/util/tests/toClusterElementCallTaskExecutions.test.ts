import {
    ClusterElementCallRecord,
    ClusterElementCallRecordStatusEnum,
    TaskExecution,
} from '@/shared/middleware/platform/workflow/execution';
import {describe, expect, it} from 'vitest';

import toClusterElementCallTaskExecutions, {
    isClusterElementCallTaskExecution,
} from '../toClusterElementCallTaskExecutions';

const createRecord = (overrides: Partial<ClusterElementCallRecord> & {id: string}): ClusterElementCallRecord => ({
    componentName: 'agent',
    startDate: new Date('2026-10-24T10:00:00Z'),
    status: ClusterElementCallRecordStatusEnum.Completed,
    toolName: overrides.id,
    workflowNodeName: 'tools_1',
    ...overrides,
});

const createTaskExecution = (
    clusterElementCallRecords?: ClusterElementCallRecord[],
    status: string = 'COMPLETED'
): TaskExecution =>
    ({
        clusterElementCallRecords,
        id: '42',
        jobId: '7',
        priority: 0,
        startDate: new Date('2026-10-24T10:00:00Z'),
        status,
    }) as TaskExecution;

describe('toClusterElementCallTaskExecutions', () => {
    it("nests a sub-agent's tool calls under its run", () => {
        const executions = toClusterElementCallTaskExecutions(
            createTaskExecution([
                createRecord({id: 't'}),
                createRecord({id: 's', parentId: 't'}),
                createRecord({id: 'f', parentId: 's'}),
                createRecord({id: 'g'}),
            ])
        );

        expect(executions.map((execution) => execution.id)).toEqual(['t', 'g']);
        expect(executions[0].children?.map((execution) => execution.id)).toEqual(['s']);
        expect(executions[0].children?.[0].children?.map((execution) => execution.id)).toEqual(['f']);
        expect(executions[0].parentTaskExecutionId).toBe('42');
        expect(executions[0].jobId).toBe('7');
        expect(executions[0].icon).toBe('/icons/agent.svg');
        expect(executions[0].workflowTask).toEqual({label: 't', name: 'tools_1'});
        expect(isClusterElementCallTaskExecution(executions[0])).toBe(true);
        expect(isClusterElementCallTaskExecution(createTaskExecution())).toBe(false);
    });

    it('numbers an element called more than once', () => {
        const executions = toClusterElementCallTaskExecutions(
            createTaskExecution([
                createRecord({
                    id: 'b',
                    startDate: new Date('2026-10-24T10:00:02Z'),
                    toolName: 'fetch_ok',
                }),
                createRecord({
                    id: 'a',
                    startDate: new Date('2026-10-24T10:00:01Z'),
                    toolName: 'fetch_ok',
                }),
                createRecord({id: 'c', toolName: 'single'}),
            ])
        );

        expect(executions.map((execution) => execution.title)).toEqual(['fetch_ok #2', 'fetch_ok #1', 'single']);
    });

    it('falls back to the workflow node name for the title', () => {
        const [execution] = toClusterElementCallTaskExecutions(
            createTaskExecution([createRecord({id: 'a', toolName: undefined})])
        );

        expect(execution.title).toBe('tools_1');
    });

    it('marks a call still running when its agent finished', () => {
        const records = [createRecord({id: 'a', status: ClusterElementCallRecordStatusEnum.Started})];

        expect(
            toClusterElementCallTaskExecutions(createTaskExecution(records, 'COMPLETED'))[0].clusterElementCallNote
        ).toBe('Still running when the agent finished');
        expect(
            toClusterElementCallTaskExecutions(createTaskExecution(records, 'STARTED'))[0].clusterElementCallNote
        ).toBeUndefined();
    });

    it('marks a suspended call as waiting', () => {
        const [execution] = toClusterElementCallTaskExecutions(
            createTaskExecution(
                [createRecord({id: 'a', status: ClusterElementCallRecordStatusEnum.Suspended})],
                'STARTED'
            )
        );

        expect(execution.clusterElementCallNote).toBe('Waiting for a response');
        expect(execution.status).toBe('STARTED');
    });

    it('maps failed calls with their error', () => {
        const [execution] = toClusterElementCallTaskExecutions(
            createTaskExecution([
                createRecord({error: 'boom', id: 'a', status: ClusterElementCallRecordStatusEnum.Failed}),
            ])
        );

        expect(execution.status).toBe('FAILED');
        expect(execution.error).toEqual({message: 'boom'});
    });

    it('parses JSON input and keeps plain text', () => {
        const [execution] = toClusterElementCallTaskExecutions(
            createTaskExecution([createRecord({id: 'a', input: '{"city":"Rome"}', output: 'sunny'})])
        );

        expect(execution.input).toEqual({city: 'Rome'});
        expect(execution.output).toBe('sunny');
    });

    it('returns no rows for a task execution without call records', () => {
        expect(toClusterElementCallTaskExecutions(createTaskExecution())).toEqual([]);
        expect(toClusterElementCallTaskExecutions(createTaskExecution([]))).toEqual([]);
    });
});
