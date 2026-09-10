import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import AiProviders from './AiProviders';

const {useGetAiProvidersQueryMock} = vi.hoisted(() => ({useGetAiProvidersQueryMock: vi.fn()}));

vi.mock('@/shared/queries/platform/aiProviders.queries', () => ({
    useGetAiProvidersQuery: useGetAiProvidersQueryMock,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 3}),
}));

vi.mock('@/pages/settings/platform/ai-providers/components/AiProviderList', () => ({
    default: ({environment}: {environment: number}) => <div data-testid="provider-list">env {environment}</div>,
}));

describe('AiProviders', () => {
    afterEach(() => {
        resetAll();
    });

    it('asks for the providers of the current environment', () => {
        useGetAiProvidersQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

        render(<AiProviders />);

        expect(useGetAiProvidersQueryMock).toHaveBeenCalledWith(3);
    });

    it('renders the list once the providers arrive', () => {
        useGetAiProvidersQueryMock.mockReturnValue({
            data: [{id: 1, name: 'openai'}],
            error: null,
            isLoading: false,
        });

        render(<AiProviders />);

        expect(screen.getByText('AI Providers')).toBeInTheDocument();
        expect(screen.getByTestId('provider-list')).toHaveTextContent('env 3');
    });

    it('does not render the list while the query is loading', () => {
        useGetAiProvidersQueryMock.mockReturnValue({data: undefined, error: null, isLoading: true});

        render(<AiProviders />);

        expect(screen.queryByTestId('provider-list')).not.toBeInTheDocument();
    });
});
