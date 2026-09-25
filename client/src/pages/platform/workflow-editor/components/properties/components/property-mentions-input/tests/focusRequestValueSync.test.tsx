vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {render} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }

    await new Promise<void>((resolve) => setTimeout(resolve, 120));
};

interface HarnessPropsI {
    focusRequest?: {initialInput?: string; token: number};
    isFormulaMode: boolean;
    onEditor: (editor: Editor | null) => void;
    value: string;
}

const Harness = ({focusRequest, isFormulaMode, onEditor, value}: HarnessPropsI) => (
    <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
        <PropertyMentionsInput
            controlType="TEXT"
            disableAutoSave
            expressionEnabled
            focusRequest={focusRequest}
            isFormulaMode={isFormulaMode}
            path="parameters.uri"
            ref={onEditor}
            setIsFormulaMode={vi.fn()}
            type="STRING"
            value={value}
        />
    </WorkflowEditorProvider>
);

beforeEach(() => {
    useWorkflowDataStore.setState({
        workflow: {id: 'wf-focus-request-sync', nodeNames: []},
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
});

describe('focus request with a new value', () => {
    it('shows the converted value when the switch turns Formula on', async () => {
        let editor: Editor | null = null;

        const handleEditor = (instance: Editor | null) => {
            editor = instance;
        };

        const view = render(<Harness isFormulaMode={false} onEditor={handleEditor} value="hello" />);

        await settle();

        view.rerender(<Harness focusRequest={{token: 1}} isFormulaMode onEditor={handleEditor} value="'hello'" />);

        await settle();

        expect(editor!.state.doc.textContent).toBe("'hello'");
        expect(editor!.isFocused).toBe(true);
    });

    it('shows the converted value after the user has typed into the editor', async () => {
        let editor: Editor | null = null;

        const handleEditor = (instance: Editor | null) => {
            editor = instance;
        };

        const view = render(<Harness isFormulaMode onEditor={handleEditor} value="concat('a', 'b')" />);

        await settle();

        act(() => {
            editor!.commands.focus('end');
        });

        await settle();

        act(() => {
            editor!.commands.insertContent(' ');
        });

        act(() => {
            editor!.commands.blur();
        });

        await settle();

        view.rerender(<Harness focusRequest={{token: 1}} isFormulaMode={false} onEditor={handleEditor} value="" />);

        await settle();

        expect(editor!.state.doc.textContent).toBe('');
    });
});
