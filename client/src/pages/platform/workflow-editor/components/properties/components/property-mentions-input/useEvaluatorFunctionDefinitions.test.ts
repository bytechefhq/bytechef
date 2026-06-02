import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useEvaluatorFunctionDefinitions} from './useEvaluatorFunctionDefinitions';

const useEvaluatorFunctionDefinitionsQueryMock = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useEvaluatorFunctionDefinitionsQuery: (...args: unknown[]) => useEvaluatorFunctionDefinitionsQueryMock(...args),
}));

describe('useEvaluatorFunctionDefinitions', () => {
    beforeEach(() => {
        useEvaluatorFunctionDefinitionsQueryMock.mockReset();
    });

    it('returns the definitions array when data is present', () => {
        const definitions = [{name: 'concat'}, {name: 'contains'}];

        useEvaluatorFunctionDefinitionsQueryMock.mockReturnValue({
            data: {evaluatorFunctionDefinitions: definitions},
        });

        const {result} = renderHook(() => useEvaluatorFunctionDefinitions());

        expect(result.current).toBe(definitions);
    });

    it('returns an empty array when data is undefined', () => {
        useEvaluatorFunctionDefinitionsQueryMock.mockReturnValue({data: undefined});

        const {result} = renderHook(() => useEvaluatorFunctionDefinitions());

        expect(result.current).toEqual([]);
    });

    it('requests with an infinite staleTime so the static catalog is fetched once', () => {
        useEvaluatorFunctionDefinitionsQueryMock.mockReturnValue({data: undefined});

        renderHook(() => useEvaluatorFunctionDefinitions());

        expect(useEvaluatorFunctionDefinitionsQueryMock).toHaveBeenCalledWith(
            {},
            expect.objectContaining({staleTime: Infinity})
        );
    });
});
