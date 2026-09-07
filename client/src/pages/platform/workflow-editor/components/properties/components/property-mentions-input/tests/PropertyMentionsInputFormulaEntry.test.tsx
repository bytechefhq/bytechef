vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {render} from '@/shared/util/test-utils';
import {act, waitFor} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {useState} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

let editor: Editor | null = null;

const Wrapper = ({autoFocus}: {autoFocus?: boolean}) => {
    const [isFormulaMode, setIsFormulaMode] = useState(false);

    return (
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <PropertyMentionsInput
                autoFocus={autoFocus}
                controlType="TEXT"
                isFormulaMode={isFormulaMode}
                label="Editor"
                leadingIcon="📄"
                path="parameters.uri"
                placeholder="https://example.com/index.html"
                setIsFormulaMode={setIsFormulaMode}
                type="STRING"
                value=""
            />
        </WorkflowEditorProvider>
    );
};

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }

    await new Promise<void>((resolve) => setTimeout(resolve, 120));
};

describe('PropertyMentionsInput', () => {
    beforeEach(() => {
        editor = null;

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-mentions-input', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
            focusedInput: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('autoFocus', () => {
        it('asks the editor for the caret when set', async () => {
            const focusSpy = vi.spyOn(HTMLElement.prototype, 'focus');

            const {container} = render(<Wrapper autoFocus />);

            await settle();

            expect(focusSpy.mock.instances).toContain(container.querySelector('.ProseMirror'));
        });

        it('leaves the caret alone when unset', async () => {
            const focusSpy = vi.spyOn(HTMLElement.prototype, 'focus');

            const {container} = render(<Wrapper />);

            await settle();

            expect(focusSpy.mock.instances).not.toContain(container.querySelector('.ProseMirror'));
        });
    });

    describe('entering formula mode by typing =', () => {
        const EditorCapture = () => {
            const [isFormulaMode, setIsFormulaMode] = useState(false);

            return (
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <PropertyMentionsInput
                        controlType="TEXT"
                        isFormulaMode={isFormulaMode}
                        label="Editor"
                        leadingIcon="📄"
                        path="parameters.uri"
                        placeholder=""
                        ref={(instance) => {
                            editor = instance;
                        }}
                        setIsFormulaMode={setIsFormulaMode}
                        type="STRING"
                        value=""
                    />
                </WorkflowEditorProvider>
            );
        };

        it('strips the = from the editor content and marks the field as formula mode', async () => {
            const {container} = render(<EditorCapture />);

            await settle();

            act(() => {
                editor!.commands.insertContent('=');
            });

            await waitFor(() =>
                expect(container.querySelector('.property-mentions-editor--formula-mode')).toBeInTheDocument()
            );

            expect(editor!.state.doc.textContent).toBe('');
        });

        it('keeps the field as the focused input', async () => {
            render(<EditorCapture />);

            await settle();

            act(() => {
                editor!.commands.insertContent('=');
            });

            await waitFor(() => expect(useWorkflowNodeDetailsPanelStore.getState().focusedInput).toBe(editor));
        });
    });

    // The control is 36px tall and the editor's line box is 20px, so 8px of padding fills it exactly. Anything
    // less left the text sitting above centre.
    it('pads the editor content to fill the control', async () => {
        const {container} = render(<Wrapper />);

        await settle();

        expect(container.querySelector('.ProseMirror')).toHaveClass('py-2');
    });
});
