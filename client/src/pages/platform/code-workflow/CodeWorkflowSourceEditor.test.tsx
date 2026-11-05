import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import {
    getCompletionContext,
    unregisterCompletionContext,
} from '@/shared/components/monaco/completion/completionContextRegistry';
import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {fireEvent, render, resetAll, screen, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import CodeWorkflowSourceEditor from './CodeWorkflowSourceEditor';

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        defaultLanguage,
        onChange,
        onMount,
        value,
    }: {
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        onMount: (editor: {getModel: () => unknown}) => void;
        value: string;
    }) => (
        <div>
            <textarea
                data-language={defaultLanguage}
                data-testid="monaco-editor-mock"
                onChange={(event) => onChange(event.target.value)}
                value={value}
            />

            <button
                onClick={() => onMount({getModel: () => ({uri: {toString: () => 'inmemory://test'}})})}
                type="button"
            >
                Mount
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/monaco/completion/useComponentCatalog', () => ({
    useComponentCatalog: () => ({components: [], getConnectionDefinition: vi.fn(), getDefinition: vi.fn()}),
}));

afterEach(() => {
    unregisterCompletionContext('inmemory://test');
    resetAll();
    vi.clearAllMocks();
});

describe('CodeWorkflowSourceEditor', () => {
    it('registers a code workflow completion context', async () => {
        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Python}
                onSave={vi.fn()}
                source=""
            />
        );

        fireEvent.click(await screen.findByRole('button', {name: 'Mount'}));

        expect(getCompletionContext('inmemory://test')).toMatchObject({kind: 'codeWorkflow', language: 'python'});
    });

    it('registers no completion context for a Java code workflow', async () => {
        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Java}
                onSave={vi.fn()}
                source=""
            />
        );

        fireEvent.click(await screen.findByRole('button', {name: 'Mount'}));

        expect(getCompletionContext('inmemory://test')).toBeUndefined();
    });

    it('renders the Monaco editor with the given source for the given language', async () => {
        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('hi');"
            />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        expect(editor).toHaveAttribute('data-language', 'javascript');
        expect(editor).toHaveValue("console.log('hi');");
    });

    it('maps Python and Ruby languages to their Monaco equivalents', async () => {
        const {rerender} = render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Python}
                onSave={vi.fn()}
                source=""
            />
        );

        expect(await screen.findByTestId('monaco-editor-mock')).toHaveAttribute('data-language', 'python');

        rerender(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Ruby}
                onSave={vi.fn()}
                source=""
            />
        );

        expect(await screen.findByTestId('monaco-editor-mock')).toHaveAttribute('data-language', 'ruby');
    });

    it('shows a loading state instead of the editor while isLoading is true', () => {
        render(
            <CodeWorkflowSourceEditor
                isLoading
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('hi');"
            />
        );

        expect(screen.queryByTestId('monaco-editor-mock')).not.toBeInTheDocument();
    });

    it('disables Save until the source is edited, and tracks dirty state', async () => {
        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('hi');"
            />
        );

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).toBeDisabled();

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: "console.log('changed');"}});

        expect(saveButton).not.toBeDisabled();
    });

    it('resets dirty state when a new source is provided', async () => {
        const {rerender} = render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('hi');"
            />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: "console.log('changed');"}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).not.toBeDisabled();

        rerender(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('new source');"
            />
        );

        expect(saveButton).toBeDisabled();
        expect(await screen.findByTestId('monaco-editor-mock')).toHaveValue("console.log('new source');");
    });

    it('is disabled while saving', () => {
        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('hi');"
            />
        );

        expect(screen.getByRole('button', {name: 'Saving...'})).toBeDisabled();
    });

    it('shows the error state instead of the editor when an error is provided', () => {
        render(
            <CodeWorkflowSourceEditor
                error={new Error('Failed to fetch source')}
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                source="console.log('hi');"
            />
        );

        expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        expect(screen.queryByTestId('monaco-editor-mock')).not.toBeInTheDocument();
    });

    it('calls onSave with the edited content on Save', async () => {
        const onSaveMock = vi.fn().mockResolvedValue(undefined);

        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={onSaveMock}
                source="console.log('hi');"
            />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: "console.log('changed');"}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        fireEvent.click(saveButton);

        expect(onSaveMock).toHaveBeenCalledWith("console.log('changed');");
    });

    it('disables Save immediately when onSave resolves, without waiting for a new source prop', async () => {
        const onSaveMock = vi.fn().mockResolvedValue(undefined);

        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={onSaveMock}
                source="console.log('hi');"
            />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: "console.log('changed');"}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).not.toBeDisabled();

        fireEvent.click(saveButton);

        await waitFor(() => expect(saveButton).toBeDisabled());
    });

    it('keeps Save enabled and remains dirty when onSave rejects', async () => {
        const onSaveMock = vi.fn().mockRejectedValue(new Error('Failed to save source'));

        render(
            <CodeWorkflowSourceEditor
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={onSaveMock}
                source="console.log('hi');"
            />
        );

        const editor = await screen.findByTestId('monaco-editor-mock');

        fireEvent.change(editor, {target: {value: "console.log('changed');"}});

        const saveButton = screen.getByRole('button', {name: 'Save'});

        fireEvent.click(saveButton);

        await waitFor(() => expect(onSaveMock).toHaveBeenCalled());

        expect(saveButton).not.toBeDisabled();
    });

    it('publishes its test configuration handler so the project header can render it', async () => {
        const onTestConfigurationClick = vi.fn();

        render(
            <CodeWorkflowSourceEditor
                headerless
                isLoading={false}
                isSaving={false}
                language={CodeWorkflowLanguage.Javascript}
                onSave={vi.fn()}
                onTestConfigurationClick={onTestConfigurationClick}
                source="({})"
            />
        );

        await waitFor(() => {
            expect(useCodeWorkflowHeaderStore.getState().onTestConfigurationClick).toBe(onTestConfigurationClick);
        });
    });
});
