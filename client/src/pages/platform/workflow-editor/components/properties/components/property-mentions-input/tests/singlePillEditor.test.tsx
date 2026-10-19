vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, waitFor} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {ComponentProps} from 'react';
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
        workflow: {id: 'wf-single-pill', nodeNames: []},
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
});

const renderSinglePill = (props: Partial<ComponentProps<typeof PropertyMentionsInput>> = {}) => {
    const onValueChange = vi.fn();
    const onSinglePillAbandoned = vi.fn();

    let editor: Editor | null = null;

    const EditorCapture = () => (
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <PropertyMentionsInput
                controlType="INTEGER"
                disableAutoSave
                expressionEnabled
                onSinglePillAbandoned={onSinglePillAbandoned}
                onValueChange={onValueChange}
                path="parameters.count"
                ref={(instance) => {
                    editor = instance;
                }}
                singlePill
                type="INTEGER"
                {...props}
            />
        </WorkflowEditorProvider>
    );

    const view = render(<EditorCapture />);

    return {getEditor: () => editor!, onSinglePillAbandoned, onValueChange, view};
};

describe('single-pill editor', () => {
    it('replaces the pill when another is inserted through the pill target', async () => {
        const {getEditor, onValueChange} = renderSinglePill({value: '${old.pill}'});

        await settle();

        act(() => {
            getEditor().commands.focus();
        });

        await settle();

        act(() => {
            useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('new.pill');
        });

        await waitFor(() => expect(onValueChange).toHaveBeenLastCalledWith('${new.pill}'));
    });

    it('takes only $ while empty', async () => {
        const {getEditor} = renderSinglePill({value: ''});

        await settle();

        const pressKey = (key: string) => {
            const keyPressEvent = new KeyboardEvent('keypress', {
                bubbles: true,
                cancelable: true,
                charCode: key.charCodeAt(0),
                key,
            });

            getEditor().view.dom.dispatchEvent(keyPressEvent);

            return keyPressEvent.defaultPrevented;
        };

        expect(pressKey('a')).toBe(true);
        expect(pressKey('5')).toBe(true);
        expect(pressKey('$')).toBe(false);
    });

    it('deletes the pill on Backspace and reports an empty value', async () => {
        const {getEditor, onValueChange} = renderSinglePill({value: '${old.pill}'});

        await settle();

        act(() => {
            getEditor().commands.focus('end');
        });

        await settle();

        fireEvent.keyDown(getEditor().view.dom, {code: 'Backspace', key: 'Backspace', keyCode: 8});

        await waitFor(() => expect(onValueChange).toHaveBeenLastCalledWith(''));

        expect(getEditor().state.doc.textContent).toBe('');
    });

    it('reports an empty value when the pill is deleted', async () => {
        const {getEditor, onValueChange} = renderSinglePill({value: '${old.pill}'});

        await settle();

        act(() => {
            getEditor().commands.clearContent(true);
        });

        await waitFor(() => expect(onValueChange).toHaveBeenLastCalledWith(''));
    });

    it('types the initial input once focus is requested', async () => {
        const {getEditor} = renderSinglePill({focusRequest: {initialInput: '$', token: 1}, value: ''});

        await settle();

        expect(getEditor().state.doc.textContent).toBe('$');
    });

    it('reports an abandoned pill entry on blur when no pill was picked', async () => {
        const {getEditor, onSinglePillAbandoned} = renderSinglePill({
            focusRequest: {initialInput: '$', token: 1},
            value: '',
        });

        await settle();

        act(() => {
            getEditor().commands.blur();
        });

        await waitFor(() => expect(onSinglePillAbandoned).toHaveBeenCalledTimes(1));
    });

    it('does not report abandonment when a pill is present', async () => {
        const {getEditor, onSinglePillAbandoned} = renderSinglePill({value: '${a.b}'});

        await settle();

        act(() => {
            getEditor().commands.focus();
            getEditor().commands.blur();
        });

        await settle();

        expect(onSinglePillAbandoned).not.toHaveBeenCalled();
    });
});
