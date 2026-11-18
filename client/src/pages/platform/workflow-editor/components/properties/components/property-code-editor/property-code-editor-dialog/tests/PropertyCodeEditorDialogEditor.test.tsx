import {
    getCompletionContext,
    unregisterCompletionContext,
} from '@/shared/components/monaco/completion/completionContextRegistry';
import {fireEvent, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogEditor from '../PropertyCodeEditorDialogEditor';

const hoisted = vi.hoisted(() => {
    return {
        catalog: {components: [], getConnectionDefinition: vi.fn(), getDefinition: vi.fn()},
        mockSetEditorValue: vi.fn(),
        storeState: {
            componentConnections: [] as Array<{componentName: string; componentVersion: number; name: string}>,
            editorValue: 'const x = 1;',
            inputParameters: undefined as Record<string, unknown> | undefined,
            scriptInput: {} as Record<string, unknown>,
        },
    };
});

vi.mock('../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            componentConnections: hoisted.storeState.componentConnections,
            editorValue: hoisted.storeState.editorValue,
            inputParameters: hoisted.storeState.inputParameters,
            scriptInput: hoisted.storeState.scriptInput,
            setEditorValue: hoisted.mockSetEditorValue,
        }),
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        className,
        defaultLanguage,
        onChange,
        onMount,
        value,
    }: {
        className?: string;
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        onMount: (editor: {focus: () => void; getModel: () => unknown}) => void;
        value?: string;
    }) => (
        <div data-testid="monaco-editor">
            <span data-testid="editor-language">{defaultLanguage}</span>

            <span data-testid="editor-value">{value}</span>

            <span data-testid="editor-class">{className}</span>

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
                    onMount({focus: vi.fn(), getModel: () => ({uri: {toString: () => 'inmemory://test'}})});
                }}
            >
                Mount
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/monaco/completion/useComponentCatalog', () => ({
    useComponentCatalog: () => hoisted.catalog,
}));

vi.mock('@/shared/components/MonacoEditorLoader', () => ({
    default: () => <div data-testid="monaco-loader">Loading...</div>,
}));

describe('PropertyCodeEditorDialogEditor', () => {
    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.editorValue = 'const x = 1;';
        hoisted.storeState.componentConnections = [];
        hoisted.storeState.inputParameters = undefined;
        hoisted.storeState.scriptInput = {};
    });

    afterEach(() => {
        unregisterCompletionContext('inmemory://test');
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

    describe('completion context', () => {
        const mountEditor = async () => {
            fireEvent.click(await screen.findByTestId('mount-btn'));
        };

        it('registers a script completion context with the dialog connections and input', async () => {
            hoisted.storeState.componentConnections = [{componentName: 'googleMaps', componentVersion: 1, name: 'gm'}];
            hoisted.storeState.scriptInput = {order: 1};

            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({
                connections: [{name: 'gm'}],
                input: {order: 1},
                kind: 'script',
                language: 'javascript',
            });
        });

        it('prefers the edited input parameters over the stored script input', async () => {
            hoisted.storeState.inputParameters = {edited: true};
            hoisted.storeState.scriptInput = {order: 1};

            render(<PropertyCodeEditorDialogEditor language="python" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({
                input: {edited: true},
                kind: 'script',
                language: 'python',
            });
        });

        it('lowercases the dialog language', async () => {
            render(<PropertyCodeEditorDialogEditor language="JAVASCRIPT" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({kind: 'script', language: 'javascript'});
        });

        it('registers no context for a Java script', async () => {
            render(<PropertyCodeEditorDialogEditor language="java" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toBeUndefined();
        });

        it('registers no context for an R script', async () => {
            render(<PropertyCodeEditorDialogEditor language="R" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toBeUndefined();
        });
    });
});
