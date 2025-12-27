import {expect, it, vi} from 'vitest';

import {FieldType, fetchTriggerFormDefinition} from './triggerForm-utils';

it('fetchTriggerFormDefinition should fetch data from the correct endpoint', async () => {
    const mockDefinition = {
        appendAttribution: true,
        ignoreBots: true,
        inputs: [
            {
                fieldName: 'testField',
                fieldType: FieldType.INPUT,
                required: true,
            },
        ],
        useWorkflowTimezone: true,
    };

    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        json: () => Promise.resolve(mockDefinition),
        ok: true,
    } as Response);

    const result = await fetchTriggerFormDefinition('test-execution-id');

    expect(fetchSpy).toHaveBeenCalledWith('/api/trigger-form/test-execution-id', {
        headers: {
            'Content-Type': 'application/json',
        },
        method: 'GET',
        signal: undefined,
    });
    expect(result).toEqual(mockDefinition);

    fetchSpy.mockRestore();
});

it('fetchTriggerFormDefinition should throw an error if the response is not ok', async () => {
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
        ok: false,
        statusText: 'Not Found',
    } as Response);

    await expect(fetchTriggerFormDefinition('test-execution-id')).rejects.toThrow(
        'Failed to load trigger definition: Not Found'
    );

    fetchSpy.mockRestore();
});
