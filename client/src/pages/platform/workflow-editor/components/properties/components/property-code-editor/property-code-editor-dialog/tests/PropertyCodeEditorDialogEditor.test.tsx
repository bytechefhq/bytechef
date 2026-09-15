import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogEditor from '../PropertyCodeEditorDialogEditor';

const hoisted = vi.hoisted(() => {
    return {
        mockSetEditorValue: vi.fn(),
        storeState: {
            editorValue: 'const x = 1;',
        },
    };
});

vi.mock('../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            editorValue: hoisted.storeState.editorValue,
            setEditorValue: hoisted.mockSetEditorValue,
        }),
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        className,
        defaultLanguage,
        onChange,
        onMount,
        options,
        value,
    }: {
        className?: string;
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        onMount: (editor: {focus: () => void}) => void;
        options?: {readOnly?: boolean};
        value?: string;
    }) => (
        <div data-testid="monaco-editor">
            <span data-testid="editor-language">{defaultLanguage}</span>

            <span data-testid="editor-value">{value}</span>

            <span data-testid="editor-class">{className}</span>

            <span data-testid="editor-read-only">{String(options?.readOnly)}</span>

            <button
                data-testid="change-value-btn"
                onClick={() => {
                    onChange('new value');
                }}
            >
                Change
            </button>

            <button
                data-testid="mount-btn"
                onClick={() => {
                    onMount({focus: vi.fn()});
                }}
            >
                Mount
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/MonacoEditorLoader', () => ({
    default: () => <div data-testid="monaco-loader">Loading...</div>,
}));

describe('PropertyCodeEditorDialogEditor', () => {
    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.editorValue = 'const x = 1;';
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render Monaco editor with correct language', async () => {
            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            const editor = await screen.findByTestId('monaco-editor');

            expect(editor).toBeInTheDocument();
            expect(screen.getByTestId('editor-language')).toHaveTextContent('javascript');
        });

        it('should render editor with correct value from store', async () => {
            hoisted.storeState.editorValue = 'function test() {}';

            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            const editorValue = await screen.findByTestId('editor-value');

            expect(editorValue).toHaveTextContent('function test() {}');
        });

        it('should render editor with correct class', async () => {
            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            const editorClass = await screen.findByTestId('editor-class');

            expect(editorClass).toHaveTextContent('size-full');
        });
    });

    describe('language prop', () => {
        it('should pass python language to editor', async () => {
            render(<PropertyCodeEditorDialogEditor language="python" />);

            const editorLanguage = await screen.findByTestId('editor-language');

            expect(editorLanguage).toHaveTextContent('python');
        });

        it('should pass ruby language to editor', async () => {
            render(<PropertyCodeEditorDialogEditor language="ruby" />);

            const editorLanguage = await screen.findByTestId('editor-language');

            expect(editorLanguage).toHaveTextContent('ruby');
        });

        it('should pass r language to editor', async () => {
            render(<PropertyCodeEditorDialogEditor language="r" />);

            const editorLanguage = await screen.findByTestId('editor-language');

            expect(editorLanguage).toHaveTextContent('r');
        });

        it('should pass java language to editor', async () => {
            render(<PropertyCodeEditorDialogEditor language="java" />);

            const editorLanguage = await screen.findByTestId('editor-language');

            expect(editorLanguage).toHaveTextContent('java');
        });
    });

    describe('onChange callback', () => {
        it('should call setEditorValue when editor value changes', async () => {
            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            const changeBtn = await screen.findByTestId('change-value-btn');
            changeBtn.click();

            expect(hoisted.mockSetEditorValue).toHaveBeenCalledWith('new value');
        });
    });

    describe('read-only mode', () => {
        const renderEditor = (readOnly: boolean) =>
            render(
                <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
                    <PropertyCodeEditorDialogEditor language="javascript" />
                </WorkflowEditorReadOnlyContext.Provider>
            );

        it('passes readOnly to Monaco and keeps the edited value when editable', async () => {
            renderEditor(false);

            expect(await screen.findByTestId('editor-read-only')).toHaveTextContent('false');

            await userEvent.click(screen.getByRole('button', {name: 'Change'}));

            expect(hoisted.mockSetEditorValue).toHaveBeenCalledWith('new value');
        });

        it('makes Monaco read-only and ignores changes in read-only mode', async () => {
            renderEditor(true);

            expect(await screen.findByTestId('editor-read-only')).toHaveTextContent('true');

            await userEvent.click(screen.getByRole('button', {name: 'Change'}));

            expect(hoisted.mockSetEditorValue).not.toHaveBeenCalled();
        });
    });
});
