import {renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import useJobExecutionOverlay from '../useJobExecutionOverlay';

describe('useJobExecutionOverlay', () => {
    it('marks the trigger that started the job as completed alongside its tasks', () => {
        const {result} = renderHook(() =>
            useJobExecutionOverlay(
                {
                    status: 'COMPLETED',
                    taskExecutions: [
                        {
                            jobId: '1',
                            priority: 0,
                            startDate: new Date(0),
                            status: 'COMPLETED',
                            workflowTask: {name: 'var_1', type: 'var/v1/set'},
                        },
                    ],
                },
                'trigger_2'
            )
        );

        expect(result.current.nodeStates.trigger_2).toEqual({status: 'COMPLETED'});
        expect(result.current.nodeStates.var_1?.status).toBe('COMPLETED');
        expect(result.current.nodeStates.trigger_1).toBeUndefined();
    });

    it('marks no trigger when the job does not say which one started it', () => {
        const {result} = renderHook(() => useJobExecutionOverlay({status: 'COMPLETED', taskExecutions: []}));

        expect(result.current.nodeStates).toEqual({});
    });
});
