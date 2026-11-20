import {act, renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import useMcpWorkflowToolSeeding from '../useMcpWorkflowToolSeeding';

describe('useMcpWorkflowToolSeeding', () => {
    it('seeds required conditional schema fields, which the workflow tool form always shows, and skips optional ones', async () => {
        const loadWorkflowProperties = async () =>
            [
                {controlType: 'TEXT', name: 'customer', required: true, type: 'STRING'},
                {controlType: 'TEXT', name: 'comment', type: 'STRING'},
                {
                    controlType: 'TEXT',
                    displayCondition: "customer != ''",
                    name: 'note',
                    required: true,
                    type: 'STRING',
                },
            ] as never;

        const {result} = renderHook(() =>
            useMcpWorkflowToolSeeding({loadWorkflowProperties, newWorkflowIds: ['workflow-1'], seedKey: '1'})
        );

        let seededValues: Record<string, Record<string, string>> = {};

        await act(async () => {
            seededValues = await result.current.waitForSeeding();
        });

        expect(Object.keys(seededValues['workflow-1']).sort()).toEqual(['customer', 'note']);
    });
});
