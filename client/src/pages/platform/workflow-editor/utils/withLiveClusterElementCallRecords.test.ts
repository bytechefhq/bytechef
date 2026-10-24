import {
    ClusterElementCallRecord,
    TaskExecution,
    WorkflowTestExecution,
} from '@/shared/middleware/platform/workflow/test';
import {describe, expect, it} from 'vitest';

import withLiveClusterElementCallRecords from './withLiveClusterElementCallRecords';

function buildExecution(taskExecution: TaskExecution): WorkflowTestExecution {
    return {job: {taskExecutions: [taskExecution]}} as unknown as WorkflowTestExecution;
}

describe('withLiveClusterElementCallRecords', () => {
    it('adds live calls the snapshot does not have yet', () => {
        const stored: ClusterElementCallRecord = {id: 'a', status: 'COMPLETED'};
        const live: ClusterElementCallRecord = {id: 'b', status: 'STARTED'};
        const execution = buildExecution({clusterElementCallRecords: [stored], id: 10} as unknown as TaskExecution);

        const merged = withLiveClusterElementCallRecords(execution, {'10': [live]});

        expect(merged?.job?.taskExecutions?.[0].clusterElementCallRecords).toEqual([stored, live]);
    });

    it('keeps the stored record over the live one', () => {
        const stored: ClusterElementCallRecord = {id: 'a', output: 'stored', status: 'COMPLETED'};
        const execution = buildExecution({clusterElementCallRecords: [stored], id: 10} as unknown as TaskExecution);

        const merged = withLiveClusterElementCallRecords(execution, {
            '10': [{id: 'a', status: 'STARTED'}],
        });

        expect(merged?.job?.taskExecutions?.[0].clusterElementCallRecords).toEqual([stored]);
    });

    it('merges into a task execution nested in an iteration', () => {
        const live: ClusterElementCallRecord = {id: 'b', status: 'STARTED'};
        const execution = buildExecution({
            id: 1,
            iterations: [[{id: 10} as unknown as TaskExecution]],
        } as unknown as TaskExecution);

        const merged = withLiveClusterElementCallRecords(execution, {'10': [live]});

        expect(merged?.job?.taskExecutions?.[0].iterations?.[0][0].clusterElementCallRecords).toEqual([live]);
    });

    it('leaves an execution without live calls untouched', () => {
        const execution = buildExecution({id: 10} as unknown as TaskExecution);

        expect(withLiveClusterElementCallRecords(execution, {})).toBe(execution);
        expect(withLiveClusterElementCallRecords(execution, {'99': [{id: 'a', status: 'STARTED'}]})).toBe(execution);
        expect(withLiveClusterElementCallRecords(undefined, {'10': []})).toBeUndefined();
    });
});
