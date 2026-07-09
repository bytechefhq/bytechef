import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useHasEnabledAiProvider} from '../useHasEnabledAiProvider';

const {useAiDefaultModelQueryMock} = vi.hoisted(() => ({
    useAiDefaultModelQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiDefaultModelQuery: useAiDefaultModelQueryMock,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

describe('useHasEnabledAiProvider', () => {
    beforeEach(() => {
        useAiDefaultModelQueryMock.mockReset();
    });

    it('reports an enabled provider when a default model resolves', () => {
        useAiDefaultModelQueryMock.mockReturnValue({
            data: {aiDefaultModel: {model: 'gpt-test', provider: 'openai'}},
            isPending: false,
        });

        const {result} = renderHook(() => useHasEnabledAiProvider());

        expect(result.current).toEqual({hasEnabledAiProvider: true, isPending: false});
        expect(useAiDefaultModelQueryMock).toHaveBeenCalledWith({environment: '1'});
    });

    it('reports no enabled provider when no default model resolves', () => {
        useAiDefaultModelQueryMock.mockReturnValue({data: {aiDefaultModel: null}, isPending: false});

        const {result} = renderHook(() => useHasEnabledAiProvider());

        expect(result.current).toEqual({hasEnabledAiProvider: false, isPending: false});
    });

    it('reports pending while the query has not resolved', () => {
        useAiDefaultModelQueryMock.mockReturnValue({data: undefined, isPending: true});

        const {result} = renderHook(() => useHasEnabledAiProvider());

        expect(result.current).toEqual({hasEnabledAiProvider: false, isPending: true});
    });
});
