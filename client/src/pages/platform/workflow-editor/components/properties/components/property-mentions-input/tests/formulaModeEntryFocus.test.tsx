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
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

const microtaskTick = async (times = 1) => {
    for (let index = 0; index < times; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }
};

let editor: Editor | null = null;

const Wrapper = () => {
    const [isFormulaMode, setIsFormulaMode] = useState(false);

    return (
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <PropertyMentionsInput
                controlType="TEXT"
                isFormulaMode={isFormulaMode}
                label="Editor"
                leadingIcon="📄"
                path="parameters.field"
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

describe('entering formula mode by typing =', () => {
    beforeEach(() => {
        editor = null;

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-formula-entry', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
            focusedInput: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    // Switching into formula mode replaces the editor's document, which drops the selection. Without an
    // explicit refocus the caret landed nowhere and the user had to click back into the field to keep typing.
    it('keeps the caret in the editor', async () => {
        render(<Wrapper />);

        await microtaskTick(2);

        expect(editor).not.toBeNull();

        act(() => {
            editor!.commands.insertContent('=');
        });

        // jsdom will not hold DOM focus on a contenteditable, so the store's focused input is what this can
        // assert - it is also what the editor chrome and the data pill panel read.
        await waitFor(() => expect(useWorkflowNodeDetailsPanelStore.getState().focusedInput).toBe(editor));
    });
});
