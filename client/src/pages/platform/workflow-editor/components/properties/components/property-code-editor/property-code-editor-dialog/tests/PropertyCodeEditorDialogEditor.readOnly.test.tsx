import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogEditor from '../PropertyCodeEditorDialogEditor';

const hoisted = vi.hoisted(() => ({
    mockSetEditorValue: vi.fn(),
}));

vi.mock('../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            editorValue: 'const x = 1;',
            setEditorValue: hoisted.mockSetEditorValue,
        }),
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({onChange, options}: {onChange: (value: string | undefined) => void; options?: {readOnly?: boolean}}) => (
        <div data-testid="monaco-editor">
            <span data-testid="editor-read-only">{String(options?.readOnly)}</span>

            <button onClick={() => onChange('changed')} type="button">
                Change
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/MonacoEditorLoader', () => ({
    default: () => <div data-testid="monaco-loader">Loading...</div>,
}));

const renderEditor = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <PropertyCodeEditorDialogEditor language="javascript" />
        </WorkflowEditorReadOnlyContext.Provider>
    );

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('PropertyCodeEditorDialogEditor in read-only mode', () => {
    it('passes readOnly to Monaco and keeps the edited value when editable', async () => {
        renderEditor(false);

        expect(await screen.findByTestId('editor-read-only')).toHaveTextContent('false');

        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        expect(hoisted.mockSetEditorValue).toHaveBeenCalledWith('changed');
    });

    it('makes Monaco read-only and ignores changes in read-only mode', async () => {
        renderEditor(true);

        expect(await screen.findByTestId('editor-read-only')).toHaveTextContent('true');

        await userEvent.click(screen.getByRole('button', {name: 'Change'}));

        expect(hoisted.mockSetEditorValue).not.toHaveBeenCalled();
    });
});
