import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {fireEvent, render, resetAll, screen, waitFor} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationCodeWorkflowDetail from './IntegrationCodeWorkflowDetail';

const hoisted = vi.hoisted(() => ({
    mockUseIntegrationCodeWorkflowSourceQuery: vi.fn(),
    mockUseUpdateIntegrationCodeWorkflowSourceMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useIntegrationCodeWorkflowSourceQuery: hoisted.mockUseIntegrationCodeWorkflowSourceQuery,
        useUpdateIntegrationCodeWorkflowSourceMutation: hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation,
    };
});

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        defaultLanguage,
        onChange,
        value,
    }: {
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        value: string;
    }) => (
        <textarea
            data-language={defaultLanguage}
            data-testid="monaco-editor-mock"
            onChange={(event) => onChange(event.target.value)}
            value={value}
        />
    ),
}));

beforeEach(() => {
    hoisted.mockUseIntegrationCodeWorkflowSourceQuery.mockReturnValue({
        data: {integrationCodeWorkflowSource: 'console.log("hi");'},
        error: null,
        isLoading: false,
    });
    hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation.mockReturnValue({
        isPending: false,
        mutateAsync: vi.fn().mockResolvedValue(undefined),
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IntegrationCodeWorkflowDetail', () => {
    it('caches its source under the key a version restore invalidates', async () => {
        const graphqlModule =
            await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

        const queryClient = new QueryClient();

        renderHook(() => graphqlModule.useIntegrationCodeWorkflowSourceQuery({integrationId: '7'}, {enabled: false}), {
            wrapper: ({children}: {children: ReactNode}) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

        const queryCache = queryClient.getQueryCache();

        expect(
            queryCache.find({exact: true, queryKey: ['integrationCodeWorkflowSource', {integrationId: '7'}]})
        ).toBeDefined();
    });

    it('fetches the source for the given integration and renders it in the editor', async () => {
        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        const editor = await screen.findByTestId('monaco-editor-mock');

        expect(editor).toHaveAttribute('data-language', 'javascript');
        expect(editor).toHaveValue('console.log("hi");');
        expect(hoisted.mockUseIntegrationCodeWorkflowSourceQuery).toHaveBeenCalledWith(
            {integrationId: '1'},
            {enabled: true}
        );
    });

    it('shows the error state when the source query returns an error', () => {
        hoisted.mockUseIntegrationCodeWorkflowSourceQuery.mockReturnValue({
            data: undefined,
            error: new Error('Failed to fetch source'),
            isLoading: false,
        });

        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        expect(screen.queryByTestId('monaco-editor-mock')).not.toBeInTheDocument();
    });

    it('reflects the mutation pending state as the saving state', () => {
        hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation.mockReturnValue({
            isPending: true,
            mutateAsync: vi.fn().mockResolvedValue(undefined),
        });

        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        expect(screen.getByRole('button', {name: 'Saving...'})).toBeDisabled();
    });

    it('calls the update mutation with the integration id and edited content on Save', async () => {
        const mutateAsyncMock = vi.fn().mockResolvedValue(undefined);

        hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation.mockReturnValue({
            isPending: false,
            mutateAsync: mutateAsyncMock,
        });

        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: 'console.log("changed");'}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        fireEvent.click(saveButton);

        expect(mutateAsyncMock).toHaveBeenCalledWith({content: 'console.log("changed");', integrationId: '1'});
    });

    it('disables Save immediately after a successful save', async () => {
        const mutateAsyncMock = vi.fn().mockResolvedValue(undefined);

        hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation.mockReturnValue({
            isPending: false,
            mutateAsync: mutateAsyncMock,
        });

        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: 'console.log("changed");'}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        fireEvent.click(saveButton);

        await waitFor(() => expect(saveButton).toBeDisabled());
    });

    it('refreshes the integrations after a save so a renamed integration shows its new name', () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        const [mutationOptions] = hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation.mock.calls[0];

        mutationOptions.onSuccess();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['integrations']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: ['integrationCodeWorkflowSource', {integrationId: '1'}],
        });

        invalidateQueriesSpy.mockRestore();
    });

    it('keeps Save enabled when the update mutation fails', async () => {
        const mutateAsyncMock = vi.fn().mockRejectedValue(new Error('Failed to save source'));

        hoisted.mockUseUpdateIntegrationCodeWorkflowSourceMutation.mockReturnValue({
            isPending: false,
            mutateAsync: mutateAsyncMock,
        });

        render(<IntegrationCodeWorkflowDetail integrationId="1" language={CodeWorkflowLanguage.Javascript} />);

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: 'console.log("changed");'}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        fireEvent.click(saveButton);

        await waitFor(() => expect(mutateAsyncMock).toHaveBeenCalled());

        expect(saveButton).not.toBeDisabled();
    });
});
