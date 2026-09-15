import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    getCompletionContext,
    unregisterCompletionContext,
} from '@/shared/components/monaco/completion/completionContextRegistry';
import {fireEvent, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogEditor from '../PropertyCodeEditorDialogEditor';

const hoisted = vi.hoisted(() => {
    return {
        catalog: {components: [], getConnectionDefinition: vi.fn(), getDefinition: vi.fn()},
        editorState: {includeComponents: undefined as string[] | undefined, platformType: 'AUTOMATION'},
        mockSetEditorValue: vi.fn(),
        mockUseComponentCatalog: vi.fn(),
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
        options,
        value,
    }: {
        className?: string;
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        onMount: (editor: {focus: () => void; getModel: () => unknown}) => void;
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
                    onMount({focus: vi.fn(), getModel: () => ({uri: {toString: () => 'inmemory://test'}})});
                }}
            >
                Mount
            </button>
        </div>
    ),
}));

vi.mock('@/shared/components/monaco/completion/useComponentCatalog', () => ({
    useComponentCatalog: (options: unknown) => {
        hoisted.mockUseComponentCatalog(options);

        return hoisted.catalog;
    },
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useWorkflowEditor: () => hoisted.editorState,
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
        hoisted.editorState.includeComponents = undefined;
        hoisted.editorState.platformType = 'AUTOMATION';

        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});
        useWorkflowEditorStore.setState({rootClusterElementNodeData: undefined});
    });

    afterEach(() => {
        unregisterCompletionContext('inmemory://test');
        resetAll();
        vi.clearAllMocks();
    });

    describe('component catalog', () => {
        it('reads the embedded catalog when the editor hosts an embedded workflow', async () => {
            hoisted.editorState.platformType = 'EMBEDDED';

            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            await screen.findByTestId('monaco-editor');

            expect(hoisted.mockUseComponentCatalog).toHaveBeenCalledWith({embedded: true});
        });

        it('reads the automation catalog otherwise', async () => {
            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            await screen.findByTestId('monaco-editor');

            expect(hoisted.mockUseComponentCatalog).toHaveBeenCalledWith({embedded: false});
        });

        it('limits the catalog to the components the host includes', async () => {
            hoisted.editorState.includeComponents = ['slack'];

            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            await screen.findByTestId('monaco-editor');

            expect(hoisted.mockUseComponentCatalog).toHaveBeenCalledWith({embedded: false, include: ['slack']});
        });
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

        it('marks a script in a non-GraalVM task runner as external', async () => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    componentName: 'script',
                    label: 'JavaScript',
                    name: 'script_1',
                    parameters: {taskRunner: {type: 'docker'}},
                    workflowNodeName: 'script_1',
                },
            } as unknown as Parameters<typeof useWorkflowNodeDetailsPanelStore.setState>[0]);

            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({externalTaskRunner: true, kind: 'script'});
        });

        it('does not mark a script in the GraalVM task runner as external', async () => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    componentName: 'script',
                    label: 'JavaScript',
                    name: 'script_1',
                    parameters: {taskRunner: {type: 'graalvm'}},
                    workflowNodeName: 'script_1',
                },
            } as unknown as Parameters<typeof useWorkflowNodeDetailsPanelStore.setState>[0]);

            render(<PropertyCodeEditorDialogEditor language="javascript" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({externalTaskRunner: false, kind: 'script'});
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

        it('registers a shell completion context with the task connections, environment and input', async () => {
            hoisted.storeState.componentConnections = [{componentName: 'slack', componentVersion: 1, name: 'slack'}];
            hoisted.storeState.scriptInput = {channel: '#general'};

            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    componentName: 'script',
                    label: 'Bash',
                    name: 'script_1',
                    parameters: {env: {API_TOKEN: 'secret', REGION: 'eu'}},
                    workflowNodeName: 'script_1',
                },
            } as unknown as Parameters<typeof useWorkflowNodeDetailsPanelStore.setState>[0]);

            render(<PropertyCodeEditorDialogEditor language="shell" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toEqual({
                connections: [{componentName: 'slack', componentVersion: 1, name: 'slack'}],
                env: ['API_TOKEN', 'REGION'],
                input: {channel: '#general'},
                kind: 'shell',
            });
        });

        it('registers a shell completion context without environment keys when the task sets none', async () => {
            render(<PropertyCodeEditorDialogEditor language="shell" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({env: [], kind: 'shell'});
        });

        it('resolves the cluster root of a script tool from the node when no root is stored', async () => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    clusterElementType: 'TOOLS',
                    componentName: 'script',
                    label: 'JavaScript',
                    name: 'script_1',
                    topLevelClusterRootId: 'aiAgent_1',
                    workflowNodeName: 'script_1',
                },
            } as unknown as Parameters<typeof useWorkflowNodeDetailsPanelStore.setState>[0]);

            render(<PropertyCodeEditorDialogEditor language="javascript" workflowNodeName="script_1" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({
                clusterElementNode: {clusterElementType: 'TOOLS', clusterElementWorkflowNodeName: 'script_1'},
                kind: 'script',
                workflowNodeName: 'aiAgent_1',
            });
        });

        it('prefers the parent cluster root of a script tool over a stale stored root', async () => {
            useWorkflowEditorStore.setState({
                rootClusterElementNodeData: {
                    componentName: 'aiAgent',
                    name: 'aiAgent_2',
                    workflowNodeName: 'aiAgent_2',
                },
            } as unknown as Parameters<typeof useWorkflowEditorStore.setState>[0]);

            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    clusterElementType: 'TOOLS',
                    componentName: 'script',
                    label: 'JavaScript',
                    name: 'script_1',
                    parentClusterRootId: 'aiAgent_1',
                    workflowNodeName: 'script_1',
                },
            } as unknown as Parameters<typeof useWorkflowNodeDetailsPanelStore.setState>[0]);

            render(<PropertyCodeEditorDialogEditor language="python" workflowNodeName="script_1" />);

            await mountEditor();

            expect(getCompletionContext('inmemory://test')).toMatchObject({
                clusterElementNode: {clusterElementType: 'TOOLS', clusterElementWorkflowNodeName: 'script_1'},
                kind: 'script',
                workflowNodeName: 'aiAgent_1',
            });
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
