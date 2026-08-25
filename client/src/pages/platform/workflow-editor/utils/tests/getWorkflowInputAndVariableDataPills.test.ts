import {describe, expect, it} from 'vitest';

import getWorkflowInputAndVariableDataPills from '../getWorkflowInputAndVariableDataPills';

describe('getWorkflowInputAndVariableDataPills', () => {
    it('builds flat input pills and vars.NAME pills', () => {
        const pills = getWorkflowInputAndVariableDataPills(
            [{name: 'customer', type: 'string'}],
            [{id: '1', name: 'API_URL', value: 'x'}]
        );

        expect(pills).toEqual([
            {id: 'customer', nodeName: 'customer', value: 'customer'},
            {id: 'vars.API_URL', nodeName: 'vars', value: 'vars.API_URL'},
        ]);
    });

    it('tolerates undefined inputs', () => {
        expect(getWorkflowInputAndVariableDataPills(undefined, [])).toEqual([]);
    });
});
