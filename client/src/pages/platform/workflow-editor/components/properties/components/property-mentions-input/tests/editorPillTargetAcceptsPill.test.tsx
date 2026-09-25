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

beforeEach(() => {
    useWorkflowDataStore.setState({
        workflow: {id: 'wf-accepts-pill', nodeNames: []},
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
});

describe('editor pill target', () => {
    it('answers acceptsPill from the current props, not the ones it was focused with', async () => {
        let editor: Editor | null = null;

        const renderEditor = (expressionEnabled: boolean) => (
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <PropertyMentionsInput
                    controlType="TEXT"
                    disableAutoSave
                    expressionEnabled={expressionEnabled}
                    path="parameters.text"
                    ref={(instance) => {
                        editor = instance;
                    }}
                    type="STRING"
                    value="hello"
                />
            </WorkflowEditorProvider>
        );

        const {rerender} = render(renderEditor(true));

        await settle();

        act(() => {
            editor!.commands.focus();
        });

        await settle();

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.acceptsPill()).toBe(true);

        rerender(renderEditor(false));

        await settle();

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.acceptsPill()).toBe(false);
    });
});
