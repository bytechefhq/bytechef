import {PropertyCopilotMode} from '@/shared/middleware/graphql-types';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useGeneratePropertyValue} from './useGeneratePropertyValue';

const mutateAsyncMock = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useGeneratePropertyValueMutation: () => ({isPending: false, mutateAsync: mutateAsyncMock}),
}));

describe('useGeneratePropertyValue', () => {
    beforeEach(() => mutateAsyncMock.mockReset());

    it('calls the mutation with the built input and returns the payload', async () => {
        mutateAsyncMock.mockResolvedValue({generatePropertyValue: {message: null, valid: true, value: '=x'}});

        const {result} = renderHook(() => useGeneratePropertyValue());

        const payload = await result.current.generate({
            dynamic: false,
            environmentId: 0,
            mode: PropertyCopilotMode.Formula,
            prompt: 'x',
            propertyPath: 'p',
            propertyType: 'STRING',
            workflowId: 'wf1',
            workflowNodeName: 'n1',
        });

        expect(mutateAsyncMock).toHaveBeenCalledWith({
            input: {
                dynamic: false,
                environmentId: 0,
                mode: PropertyCopilotMode.Formula,
                prompt: 'x',
                propertyPath: 'p',
                propertyType: 'STRING',
                workflowId: 'wf1',
                workflowNodeName: 'n1',
            },
        });
        expect(payload).toEqual({message: null, valid: true, value: '=x'});
    });
});
