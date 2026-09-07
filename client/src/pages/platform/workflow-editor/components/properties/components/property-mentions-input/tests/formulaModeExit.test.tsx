vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {render, screen} from '@/shared/util/test-utils';
import {fireEvent, waitFor} from '@testing-library/react';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

const microtaskTick = async (times = 1) => {
    for (let index = 0; index < times; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }
};

// Mirrors the uncontrolled Property wiring: formula mode is React state owned above the input.
const StatefulWrapper = ({onFormulaModeChange}: {onFormulaModeChange: (isFormulaMode: boolean) => void}) => {
    const [isFormulaMode, setIsFormulaMode] = useState(true);

    return (
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <PropertyMentionsInput
                controlType="TEXT"
                isFormulaMode={isFormulaMode}
                label="Editor"
                leadingIcon="📄"
                path="parameters.field"
                placeholder=""
                setIsFormulaMode={(value) => {
                    onFormulaModeChange(value);

                    setIsFormulaMode(value);
                }}
                type="STRING"
                value=""
            />
        </WorkflowEditorProvider>
    );
};

const pressKey = (element: Element, key: string, keyCode: number) =>
    fireEvent.keyDown(element, {charCode: keyCode, code: key, key, keyCode});

describe('leaving formula mode by emptying the field', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-formula-exit', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('exits formula mode when backspace lands on an empty editor', async () => {
        const onFormulaModeChange = vi.fn();

        render(<StatefulWrapper onFormulaModeChange={onFormulaModeChange} />);

        await microtaskTick(2);

        pressKey(screen.getByRole('textbox', {name: 'Editor'}), 'Backspace', 8);

        await waitFor(() => expect(onFormulaModeChange).toHaveBeenCalledWith(false));
    });

    // Which key emptied the field is not the user's concern — the Delete key has to leave formula mode the
    // same way Backspace does.
    it('exits formula mode when delete lands on an empty editor', async () => {
        const onFormulaModeChange = vi.fn();

        render(<StatefulWrapper onFormulaModeChange={onFormulaModeChange} />);

        await microtaskTick(2);

        pressKey(screen.getByRole('textbox', {name: 'Editor'}), 'Delete', 46);

        await waitFor(() => expect(onFormulaModeChange).toHaveBeenCalledWith(false));
    });

    // The exit re-renders the field around the editor - the leading icon, the hint and the help note all
    // change. The caret was in the editor when the key was pressed and belongs there afterwards. jsdom will
    // not hold focus on a contenteditable, so what this asserts is that the node is asked for it.
    it('restores the caret to the editor after leaving formula mode', async () => {
        const focusSpy = vi.spyOn(HTMLElement.prototype, 'focus');

        const {container} = render(<StatefulWrapper onFormulaModeChange={vi.fn()} />);

        await microtaskTick(2);

        pressKey(screen.getByRole('textbox', {name: 'Editor'}), 'Backspace', 8);

        await microtaskTick(2);
        await new Promise<void>((resolve) => setTimeout(resolve, 120));
        await microtaskTick(2);

        expect(focusSpy.mock.instances).toContain(container.querySelector('.ProseMirror'));

        focusSpy.mockRestore();
    });
});
