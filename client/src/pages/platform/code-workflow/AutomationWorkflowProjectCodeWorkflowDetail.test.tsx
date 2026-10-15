import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {fireEvent, render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectCodeWorkflowDetail from './AutomationWorkflowProjectCodeWorkflowDetail';

const hoisted = vi.hoisted(() => ({
    mockUseAutomationWorkflowProjectCodeWorkflowSourceQuery: vi.fn(),
    mockUseUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useAutomationWorkflowProjectCodeWorkflowSourceQuery:
            hoisted.mockUseAutomationWorkflowProjectCodeWorkflowSourceQuery,
        useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation:
            hoisted.mockUseUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation,
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
    hoisted.mockUseAutomationWorkflowProjectCodeWorkflowSourceQuery.mockReturnValue({
        data: {
            automationWorkflowProjectCodeWorkflowSource: {
                content: 'console.log("hi");',
                language: CodeWorkflowLanguage.Javascript,
            },
        },
        error: null,
        isLoading: false,
    });
    hoisted.mockUseUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation.mockReturnValue({
        isPending: false,
        mutateAsync: vi.fn().mockResolvedValue(undefined),
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AutomationWorkflowProjectCodeWorkflowDetail', () => {
    it('fetches the source for the given project and renders it in the editor', async () => {
        render(
            <AutomationWorkflowProjectCodeWorkflowDetail
                automationWorkflowProjectId="7"
                language={CodeWorkflowLanguage.Javascript}
            />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        expect(editor).toHaveAttribute('data-language', 'javascript');
        expect(editor).toHaveValue('console.log("hi");');
        expect(hoisted.mockUseAutomationWorkflowProjectCodeWorkflowSourceQuery).toHaveBeenCalledWith(
            {automationWorkflowProjectId: '7'},
            {enabled: true}
        );
    });

    it('calls the update mutation with the project id and edited content on Save', async () => {
        const mutateAsyncMock = vi.fn().mockResolvedValue(undefined);

        hoisted.mockUseUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation.mockReturnValue({
            isPending: false,
            mutateAsync: mutateAsyncMock,
        });

        render(
            <AutomationWorkflowProjectCodeWorkflowDetail
                automationWorkflowProjectId="7"
                language={CodeWorkflowLanguage.Javascript}
            />
        );

        fireEvent.change(await screen.findByTestId('monaco-editor-mock'), {
            target: {value: 'console.log("changed");'},
        });

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateAsyncMock).toHaveBeenCalledWith({
            automationWorkflowProjectId: '7',
            content: 'console.log("changed");',
        });
    });
});
