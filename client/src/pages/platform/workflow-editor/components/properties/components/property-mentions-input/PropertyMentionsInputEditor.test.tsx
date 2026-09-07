vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

vi.mock('use-debounce', () => {
    type FnType = (...args: unknown[]) => unknown;
    type DebouncedType<T extends FnType> = ((...args: Parameters<T>) => ReturnType<T>) & {
        cancel: () => void;
        flush: () => void;
    };

    const useDebouncedCallback = <T extends FnType>(fn: T): DebouncedType<T> => {
        const stateRef = React.useRef<{
            fn: T;
            lastArgs: Parameters<T> | null;
            timer: ReturnType<typeof setTimeout> | null;
        }>({fn, lastArgs: null, timer: null});

        stateRef.current.fn = fn;

        const run = React.useCallback(() => {
            const argsToUse = stateRef.current.lastArgs;

            stateRef.current.lastArgs = null;
            stateRef.current.timer = null;

            if (argsToUse) {
                return stateRef.current.fn(...(argsToUse as Parameters<T>)) as ReturnType<T>;
            }

            return undefined as ReturnType<T>;
        }, []);

        const debounced = React.useMemo(() => {
            const fnDebounced = ((...args: Parameters<T>) => {
                stateRef.current.lastArgs = args;

                if (stateRef.current.timer == null) {
                    stateRef.current.timer = setTimeout(run, 0);
                }

                return undefined as ReturnType<T>;
            }) as DebouncedType<T>;

            fnDebounced.cancel = () => {
                if (stateRef.current.timer != null) {
                    clearTimeout(stateRef.current.timer);
                    stateRef.current.timer = null;
                }

                stateRef.current.lastArgs = null as Parameters<T> | null;
            };
            fnDebounced.flush = () => {
                if (stateRef.current.timer != null) {
                    clearTimeout(stateRef.current.timer);
                    run();
                }
            };

            return fnDebounced;
        }, [run]);

        React.useEffect(() => {
            const state = stateRef.current;

            return () => {
                if (state.timer != null) {
                    clearTimeout(state.timer);
                    state.timer = null;
                }

                state.lastArgs = null;
            };
        }, []);

        return debounced;
    };

    return {useDebouncedCallback};
});

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {waitFor} from '@testing-library/react';
import * as React from 'react';
import {type Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from './PropertyMentionsInput';
import {FORMULA_MODE_PLACEHOLDER} from './mentionsInputPlaceholder';

const microtaskTick = async (times = 1) => {
    for (let index = 0; index < times; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }
};

const renderEditor = (props: Partial<React.ComponentProps<typeof PropertyMentionsInput>> = {}) =>
    render(
        <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
            <PropertyMentionsInput
                controlType="TEXT"
                label="Editor"
                leadingIcon="📄"
                path="parameters.field"
                placeholder=""
                type="STRING"
                value=""
                {...props}
            />
        </WorkflowEditorProvider>
    );

beforeEach(() => {
    useWorkflowDataStore.setState({
        workflow: {
            id: 'wf-editor-test',
            nodeNames: ['trigger_1'],
        },
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentComponent: undefined,
        currentNode: {
            connectionId: undefined,
            workflowNodeName: 'test_1',
        },
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

    (saveProperty as unknown as Mock).mockReset();
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('PropertyMentionsInputEditor', () => {
    describe('editorValue initialization — = prefix stripping', () => {
        it('should strip leading = from expression value and display content without it', async () => {
            renderEditor({value: '=fromAi(name)'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            expect(textbox.textContent).toBe('fromAi(name)');
        });

        it('should keep plain string value as-is', async () => {
            renderEditor({value: 'hello world'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            expect(textbox.textContent).toBe('hello world');
        });

        it('should render empty content for empty value', async () => {
            renderEditor({value: ''});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            expect(textbox.textContent).toBe('');
        });
    });

    describe('value sync effect', () => {
        it('should update editor content when value prop changes externally', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="SyncEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="initial"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'SyncEditor'}).textContent).toBe('initial');

            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="SyncEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="updated"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'SyncEditor'}).textContent).toBe('updated');
        });

        it('should strip = prefix when value prop changes to an expression', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="ExprEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="plain"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'ExprEditor'}).textContent).toBe('plain');

            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="ExprEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="=someExpression"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'ExprEditor'}).textContent).toBe('someExpression');
        });
    });

    describe('focus guard — prevents value overwrite during typing', () => {
        it('should not overwrite editor content with stale value prop while focused', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="FocusGuard"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="initial"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'FocusGuard'});

            expect(textbox.textContent).toBe('initial');

            // Focus the editor (simulates user clicking into the field)
            const user = userEvent.setup();

            await user.click(textbox);

            // Type additional characters
            await user.type(textbox, 'XYZ');

            await microtaskTick(2);

            // Simulate server response arriving with the old saved value
            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="FocusGuard"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="initial"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            // Editor should retain the user's typed content, NOT revert to "initial"
            expect(textbox.textContent).toContain('XYZ');
        });

        it('should sync value prop after editor loses focus', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="BlurSync"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="before"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'BlurSync'});

            expect(textbox.textContent).toBe('before');

            // Focus, type, then blur
            const user = userEvent.setup();

            await user.click(textbox);
            await user.type(textbox, '!!');

            // Click outside to blur the editor
            await user.click(document.body);

            await microtaskTick(2);

            // Now rerender with a new external value — should sync because editor is blurred
            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="BlurSync"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="after"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(textbox.textContent).toBe('after');
        });

        it('should allow external sync when editor was never focused', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="NoFocus"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="original"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'NoFocus'}).textContent).toBe('original');

            // Rerender with new value without ever focusing the editor
            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        label="NoFocus"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="externally updated"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'NoFocus'}).textContent).toBe('externally updated');
        });
    });

    describe('isFromAi behavior', () => {
        it('should show overlay message instead of editor when isFromAi is true', async () => {
            renderEditor({isFromAi: true, value: 'fromAi content'});

            await microtaskTick(2);

            expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument();
            expect(screen.queryByRole('textbox', {name: 'Editor'})).not.toBeInTheDocument();
        });

        it('should not disable the editor when isFromAi is false', async () => {
            renderEditor({isFromAi: false, value: 'editable content'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});
            const editorContent = textbox.closest('.ProseMirror')?.parentElement;

            expect(editorContent).not.toHaveAttribute('disabled');
        });

        it('should not save when isFromAi is true', async () => {
            const savePropertyMock = saveProperty as unknown as Mock;

            savePropertyMock.mockImplementation(() => undefined);

            const {unmount} = renderEditor({isFromAi: true, value: ''});

            await microtaskTick(2);

            expect(screen.queryByRole('textbox', {name: 'Editor'})).not.toBeInTheDocument();
            expect(savePropertyMock).not.toHaveBeenCalled();

            unmount();
        });
    });

    describe('content processing', () => {
        it('should render multiline text with paragraph elements', async () => {
            renderEditor({controlType: 'TEXT_AREA', value: 'line1\nline2\nline3'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});
            const paragraphs = textbox.querySelectorAll('p');

            expect(paragraphs).toHaveLength(3);
            expect(paragraphs[0].textContent).toBe('line1');
            expect(paragraphs[1].textContent).toBe('line2');
            expect(paragraphs[2].textContent).toBe('line3');
        });

        it('should render data pill syntax as mention spans', async () => {
            renderEditor({value: 'Hello ${trigger_1.name}'});

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            await waitFor(() => {
                expect(textbox.querySelector('.property-mention')).toBeInTheDocument();
            });
        });

        it('should render plain text without data pills as plain text', async () => {
            renderEditor({value: 'just plain text'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            expect(textbox.textContent).toBe('just plain text');
            expect(textbox.querySelector('.property-mention')).not.toBeInTheDocument();
        });
    });

    describe('formula mode', () => {
        it('should strip = prefix for formula mode values', async () => {
            renderEditor({isFormulaMode: true, value: '=1 + 2'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            expect(textbox.textContent).toBe('1 + 2');
        });

        it('should strip = from value when formula mode value changes externally', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={true}
                        label="FormulaEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="=1 + 2"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'FormulaEditor'}).textContent).toBe('1 + 2');

            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={true}
                        label="FormulaEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="=3 + 4"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'FormulaEditor'}).textContent).toBe('3 + 4');
        });

        it('should render the editor content in a monospace font in formula mode', async () => {
            renderEditor({isFormulaMode: true, value: '=1 + 2'});

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'Editor'})).toHaveClass('font-mono');
        });

        it('should not render the editor content in a monospace font outside formula mode', async () => {
            renderEditor({isFormulaMode: false, value: 'plain'});

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'Editor'})).not.toHaveClass('font-mono');
        });

        // The editor is created once, so the monospace class only tracks the toggle if tiptap's
        // useEditor effect reapplies editorProps.attributes through setOptions on rerender.
        it('should apply the monospace font when formula mode is switched on', async () => {
            const {rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={false}
                        label="FormulaEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="plain"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'FormulaEditor'})).not.toHaveClass('font-mono');

            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={true}
                        label="FormulaEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        placeholder=""
                        type="STRING"
                        value="plain"
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'FormulaEditor'})).toHaveClass('font-mono');
        });

        // 13px mono reads alongside the 14px sans of every other property value: 14px mono was too heavy,
        // 12px too faint. The /5 pins the leading so the field holds its height across the toggle.
        it('should set the editor content to 13px mono in formula mode', async () => {
            renderEditor({isFormulaMode: true, value: '=1 + 2'});

            await microtaskTick(2);

            const textbox = screen.getByRole('textbox', {name: 'Editor'});

            expect(textbox).toHaveClass('text-[13px]/5');
            expect(textbox).not.toHaveClass('text-sm');
        });

        it('should keep the editor content at text-sm outside formula mode', async () => {
            renderEditor({isFormulaMode: false, value: 'plain'});

            await microtaskTick(2);

            expect(screen.getByRole('textbox', {name: 'Editor'})).toHaveClass('text-sm');
        });

        // Data pill chips size themselves, so the editor's own text size does not reach them. The
        // modifier on the wrapper is what the stylesheet hangs the matching chip size off.
        it('should mark the editor wrapper so data pill chips can shrink with the text', async () => {
            const {container} = renderEditor({isFormulaMode: true, value: '=1 + 2'});

            await microtaskTick(2);

            expect(container.querySelector('.property-mentions-editor--formula-mode')).toBeInTheDocument();
        });

        it('should not mark the editor wrapper outside formula mode', async () => {
            const {container} = renderEditor({isFormulaMode: false, value: 'plain'});

            await microtaskTick(2);

            expect(container.querySelector('.property-mentions-editor--formula-mode')).not.toBeInTheDocument();
        });

        it('should show the expression sample placeholder in formula mode', async () => {
            const {container} = renderEditor({isFormulaMode: true, value: ''});

            await microtaskTick(2);

            expect(container.querySelector('[data-placeholder]')).toHaveAttribute(
                'data-placeholder',
                FORMULA_MODE_PLACEHOLDER
            );
        });

        // The placeholder is resolved per decoration rather than baked into the extensions memo, so
        // toggling formula mode has to swap it without the editor being rebuilt.
        it('should swap the placeholder when formula mode is switched on', async () => {
            const {container, rerender} = render(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={false}
                        label="FormulaEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        type="STRING"
                        value=""
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(container.querySelector('[data-placeholder]')).not.toHaveAttribute(
                'data-placeholder',
                FORMULA_MODE_PLACEHOLDER
            );

            rerender(
                <WorkflowEditorProvider value={workflowEditorProviderTestValue}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={true}
                        label="FormulaEditor"
                        leadingIcon="📄"
                        path="parameters.field"
                        type="STRING"
                        value=""
                    />
                </WorkflowEditorProvider>
            );

            await microtaskTick(2);

            expect(container.querySelector('[data-placeholder]')).toHaveAttribute(
                'data-placeholder',
                FORMULA_MODE_PLACEHOLDER
            );
        });
    });

    describe('FromAi toggle button', () => {
        it('should not render FromAiToggleButton when clusterElementType is not tools', async () => {
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {
                    clusterElementType: undefined,
                    connectionId: undefined,
                    workflowNodeName: 'test_1',
                },
            } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

            renderEditor({handleFromAiClick: vi.fn(), isFromAi: false, value: 'test'});

            await microtaskTick(2);

            expect(screen.queryByTitle('Generate content with AI')).not.toBeInTheDocument();
            expect(screen.queryByTitle('Customize AI generation')).not.toBeInTheDocument();
        });
    });
});
