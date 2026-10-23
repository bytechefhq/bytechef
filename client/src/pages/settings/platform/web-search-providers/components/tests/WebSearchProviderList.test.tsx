import {WebSearchProvidersQuery} from '@/shared/middleware/graphql';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WebSearchProviderList from '../WebSearchProviderList';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    enableMutate: vi.fn(),
    testMutate: vi.fn(),
    testMutation: {
        data: undefined as {testWebSearchProvider: {errorMessage: string | null; success: boolean}} | undefined,
        variables: undefined as {name: string} | undefined,
    },
    testReset: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteWebSearchProviderMutation: () => ({isPending: false, mutate: hoisted.deleteMutate}),
    useEnableWebSearchProviderMutation: () => ({isPending: false, mutate: hoisted.enableMutate}),
    useTestWebSearchProviderMutation: () => ({
        data: hoisted.testMutation.data,
        isPending: false,
        mutate: hoisted.testMutate,
        reset: hoisted.testReset,
        variables: hoisted.testMutation.variables,
    }),
    useUpdateWebSearchProviderMutation: () => ({isPending: false, mutate: hoisted.updateMutate}),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="provider icon" src={src} />,
}));

type WebSearchProviderType = WebSearchProvidersQuery['webSearchProviders'][number];

const brave: WebSearchProviderType = {
    apiKeyFromProperties: false,
    baseUrl: 'https://api.search.brave.com/res/v1',
    defaultBaseUrl: 'https://api.search.brave.com/res/v1',
    description: 'Fast web search that returns ranked result snippets.',
    enabled: true,
    icon: '/icons/brave.svg',
    label: 'Brave Search',
    maskedApiKey: '********1234',
    name: 'brave',
};

const firecrawl: WebSearchProviderType = {
    apiKeyFromProperties: false,
    baseUrl: 'https://api.firecrawl.dev/v2',
    defaultBaseUrl: 'https://api.firecrawl.dev/v2',
    description: 'Web search, web page scraping and website mapping.',
    enabled: false,
    icon: null,
    label: 'Firecrawl',
    maskedApiKey: null,
    name: 'firecrawl',
};

const renderList = (webSearchProviders: WebSearchProviderType[]) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    const wrapper = ({children}: {children: ReactNode}) => <QueryClientWrapper>{children}</QueryClientWrapper>;

    return render(<WebSearchProviderList environment={1} webSearchProviders={webSearchProviders} />, {wrapper});
};

describe('WebSearchProviderList', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.testMutation.data = undefined;
        hoisted.testMutation.variables = undefined;
    });

    it('shows each provider with its description and only the masked key', () => {
        renderList([brave, firecrawl]);

        expect(screen.getByText('Brave Search')).toBeInTheDocument();
        expect(screen.getByText('Fast web search that returns ranked result snippets.')).toBeInTheDocument();
        expect(screen.getByText('Firecrawl')).toBeInTheDocument();

        fireEvent.click(screen.getByText('Brave Search'));

        expect(screen.getByText('********1234')).toBeInTheDocument();
        expect(screen.queryByText('Base URL:')).not.toBeInTheDocument();
    });

    it('shows a base URL that differs from the default', () => {
        renderList([{...brave, baseUrl: 'https://brave.proxy/res/v1'}]);

        fireEvent.click(screen.getByText('Brave Search'));

        expect(screen.getByText('https://brave.proxy/res/v1')).toBeInTheDocument();
    });

    it('asks for a key instead of enabling a provider that has none', () => {
        renderList([firecrawl]);

        fireEvent.click(screen.getByRole('switch', {name: 'Enable Firecrawl'}));

        expect(hoisted.enableMutate).not.toHaveBeenCalled();
        expect(screen.getByLabelText('API Key')).toBeInTheDocument();
    });

    it('switches off a provider that has a key', () => {
        renderList([brave]);

        fireEvent.click(screen.getByRole('switch', {name: 'Enable Brave Search'}));

        expect(hoisted.enableMutate).toHaveBeenCalledWith({enabled: false, environment: '1', name: 'brave'});
    });

    it('removes a saved key but never a key from the application properties', () => {
        const {unmount} = renderList([brave]);

        fireEvent.click(screen.getByText('Brave Search'));
        fireEvent.click(screen.getByRole('button', {name: 'Remove'}));

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({environment: '1', name: 'brave'});

        unmount();

        renderList([{...brave, apiKeyFromProperties: true}]);

        fireEvent.click(screen.getByText('Brave Search'));

        expect(screen.getByText('Application properties')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Remove'})).not.toBeInTheDocument();
    });

    it('tests the saved key and shows the outcome under that provider', () => {
        hoisted.testMutation.data = {testWebSearchProvider: {errorMessage: '401 Unauthorized', success: false}};
        hoisted.testMutation.variables = {name: 'brave'};

        renderList([brave]);

        fireEvent.click(screen.getByText('Brave Search'));
        fireEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(hoisted.testMutate).toHaveBeenCalledWith({environment: '1', name: 'brave'});
        expect(screen.getByRole('status')).toHaveTextContent('401 Unauthorized');
    });

    it('clears a shown test result when the provider is edited or the typed values change', () => {
        renderList([brave]);

        fireEvent.click(screen.getByText('Brave Search'));
        fireEvent.click(screen.getByRole('button', {name: 'Edit'}));

        expect(hoisted.testReset).toHaveBeenCalledTimes(1);

        fireEvent.change(screen.getByLabelText('API Key'), {target: {value: 'other-key'}});

        expect(hoisted.testReset).toHaveBeenCalledTimes(2);
    });

    it('saves the trimmed key and base URL entered for a provider', async () => {
        renderList([firecrawl]);

        fireEvent.click(screen.getByText('Firecrawl'));
        fireEvent.change(screen.getByLabelText('API Key'), {target: {value: '  fc-key  '}});
        fireEvent.change(screen.getByLabelText('Base URL'), {target: {value: ' https://firecrawl.internal/v2 '}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateMutate).toHaveBeenCalledWith({
                apiKey: 'fc-key',
                baseUrl: 'https://firecrawl.internal/v2',
                environment: '1',
                name: 'firecrawl',
            })
        );
    });

    it('keeps the current key when only the base URL changes', async () => {
        renderList([brave]);

        fireEvent.click(screen.getByText('Brave Search'));
        fireEvent.click(screen.getByRole('button', {name: 'Edit'}));
        fireEvent.change(screen.getByLabelText('Base URL'), {target: {value: 'https://brave.proxy/res/v1'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateMutate).toHaveBeenCalledWith({
                apiKey: null,
                baseUrl: 'https://brave.proxy/res/v1',
                environment: '1',
                name: 'brave',
            })
        );
    });

    it('tests the typed key before it is saved', async () => {
        renderList([firecrawl]);

        fireEvent.click(screen.getByText('Firecrawl'));
        fireEvent.change(screen.getByLabelText('API Key'), {target: {value: 'fc-key'}});
        fireEvent.click(screen.getByRole('button', {name: 'Test'}));

        await waitFor(() =>
            expect(hoisted.testMutate).toHaveBeenCalledWith({
                apiKey: 'fc-key',
                baseUrl: null,
                environment: '1',
                name: 'firecrawl',
            })
        );
        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });

    it('does not save an empty key or a base URL that is not http or https', async () => {
        renderList([firecrawl]);

        fireEvent.click(screen.getByText('Firecrawl'));
        fireEvent.change(screen.getByLabelText('Base URL'), {target: {value: 'ftp://firecrawl.internal'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('API Key is required.')).toBeInTheDocument();
        expect(screen.getByText('Enter an http or https URL.')).toBeInTheDocument();
        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });
});
