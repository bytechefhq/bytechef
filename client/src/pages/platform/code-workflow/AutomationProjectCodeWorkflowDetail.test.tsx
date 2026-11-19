import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {fireEvent, render, resetAll, screen} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectCodeWorkflowDetail from './AutomationProjectCodeWorkflowDetail';

const hoisted = vi.hoisted(() => ({
    mockUseAutomationProjectCodeWorkflowSourceQuery: vi.fn(),
    mockUseUpdateAutomationProjectCodeWorkflowSourceMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useAutomationProjectCodeWorkflowSourceQuery: hoisted.mockUseAutomationProjectCodeWorkflowSourceQuery,
        useUpdateAutomationProjectCodeWorkflowSourceMutation:
            hoisted.mockUseUpdateAutomationProjectCodeWorkflowSourceMutation,
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
    hoisted.mockUseAutomationProjectCodeWorkflowSourceQuery.mockReturnValue({
        data: {
            automationProjectCodeWorkflowSource: {
                content: 'console.log("hi");',
                language: CodeWorkflowLanguage.Javascript,
            },
        },
        error: null,
        isLoading: false,
    });
    hoisted.mockUseUpdateAutomationProjectCodeWorkflowSourceMutation.mockReturnValue({
        isPending: false,
        mutateAsync: vi.fn().mockResolvedValue(undefined),
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AutomationProjectCodeWorkflowDetail', () => {
    it('caches its source under the key a version restore invalidates', async () => {
        const graphqlModule =
            await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

        const queryClient = new QueryClient();

        renderHook(
            () =>
                graphqlModule.useAutomationProjectCodeWorkflowSourceQuery({automationProjectId: '7'}, {enabled: false}),
            {
                wrapper: ({children}: {children: ReactNode}) => (
                    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
                ),
            }
        );

        const queryCache = queryClient.getQueryCache();

        expect(
            queryCache.find({
                exact: true,
                queryKey: ['automationProjectCodeWorkflowSource', {automationProjectId: '7'}],
            })
        ).toBeDefined();
    });

    it('fetches the source for the given project and renders it in the editor', async () => {
        render(
            <AutomationProjectCodeWorkflowDetail automationProjectId="7" language={CodeWorkflowLanguage.Javascript} />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        expect(editor).toHaveAttribute('data-language', 'javascript');
        expect(editor).toHaveValue('console.log("hi");');
        expect(hoisted.mockUseAutomationProjectCodeWorkflowSourceQuery).toHaveBeenCalledWith(
            {automationProjectId: '7'},
            {enabled: true}
        );
    });

    it('calls the update mutation with the project id and edited content on Save', async () => {
        const mutateAsyncMock = vi.fn().mockResolvedValue(undefined);

        hoisted.mockUseUpdateAutomationProjectCodeWorkflowSourceMutation.mockReturnValue({
            isPending: false,
            mutateAsync: mutateAsyncMock,
        });

        render(
            <AutomationProjectCodeWorkflowDetail automationProjectId="7" language={CodeWorkflowLanguage.Javascript} />
        );

        fireEvent.change(await screen.findByTestId('monaco-editor-mock'), {
            target: {value: 'console.log("changed");'},
        });

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateAsyncMock).toHaveBeenCalledWith({
            automationProjectId: '7',
            content: 'console.log("changed");',
        });
    });
});
