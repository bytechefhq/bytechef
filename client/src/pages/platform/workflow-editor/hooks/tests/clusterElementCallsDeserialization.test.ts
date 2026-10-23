import {TaskExecutionFromJSON as EmbeddedTaskExecutionFromJSON} from '@/ee/shared/middleware/embedded/workflow/execution/models/TaskExecution';
import {TaskExecutionFromJSON as AutomationTaskExecutionFromJSON} from '@/shared/middleware/automation/workflow/execution/models/TaskExecution';
import {describe, expect, it} from 'vitest';

const TASK_EXECUTION_JSON = {
    clusterElementCalls: [
        {
            endDate: '2026-10-23T10:00:00.412Z',
            name: 'httpClient_1',
            startDate: '2026-10-23T10:00:00Z',
            status: 'COMPLETED',
        },
    ],
    jobId: '1',
    priority: 0,
    startDate: '2026-10-23T10:00:00Z',
    status: 'COMPLETED',
};

describe('cluster element calls deserialization', () => {
    it('keeps the stored calls of an automation task execution', () => {
        expect(AutomationTaskExecutionFromJSON(TASK_EXECUTION_JSON).clusterElementCalls).toEqual([
            expect.objectContaining({name: 'httpClient_1', status: 'COMPLETED'}),
        ]);
    });

    it('keeps the stored calls of an embedded task execution', () => {
        expect(EmbeddedTaskExecutionFromJSON(TASK_EXECUTION_JSON).clusterElementCalls).toEqual([
            expect.objectContaining({name: 'httpClient_1', status: 'COMPLETED'}),
        ]);
    });
});
