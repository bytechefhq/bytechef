import {describe, expect, it} from 'vitest';

import {getVariablesApi, registerVariablesApi} from './variablesApi';

describe('variablesApi', () => {
    it('defaults to a no-op query that reports no data', () => {
        const {data} = getVariablesApi().useWorkflowVariablesQuery({type: 'EMBEDDED'}, 0);

        expect(data).toBeUndefined();
    });

    it('returns the registered implementation', () => {
        const registered = {
            useWorkflowVariablesQuery: () => ({data: [{id: '1', name: 'A', value: '1'}]}),
        };

        registerVariablesApi(registered);

        expect(getVariablesApi()).toBe(registered);
    });
});
