vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {ControlType} from '@/shared/middleware/platform/configuration';
import {render} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {MutableRefObject} from 'react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }

    await new Promise<void>((resolve) => setTimeout(resolve, 120));
};

const waitPastSaveDebounce = () => new Promise<void>((resolve) => setTimeout(resolve, 800));

interface HarnessPropsI {
    cancelPendingSaveRef?: MutableRefObject<(() => void) | null>;
    controlType: ControlType;
    focusRequest?: {initialInput?: string; token: number};
    isFormulaMode: boolean;
    onEditor: (editor: Editor | null) => void;
    type: string;
    value: string;
}

const Harness = ({
    cancelPendingSaveRef,
    controlType,
    focusRequest,
    isFormulaMode,
    onEditor,
    type,
    value,
}: HarnessPropsI) => (
    <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
        <PropertyMentionsInput
            cancelPendingSaveRef={cancelPendingSaveRef}
            controlType={controlType}
            expressionEnabled
            focusRequest={focusRequest}
            isFormulaMode={isFormulaMode}
            path="parameters.field"
            ref={onEditor}
            setIsFormulaMode={vi.fn()}
            type={type}
            value={value}
        />
    </WorkflowEditorProvider>
);

const savedValues = () => (saveProperty as unknown as Mock).mock.calls.map(([options]) => options.value);

beforeEach(() => {
    (saveProperty as unknown as Mock).mockReset();

    useWorkflowDataStore.setState({
        workflow: {id: 'wf-pending-save', nodeNames: []},
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
});

describe('a pending editor save when the mode switches', () => {
    it('is dropped when Formula turns on while the editor stays mounted', async () => {
        let editor: Editor | null = null;

        const handleEditor = (instance: Editor | null) => {
            editor = instance;
        };

        const view = render(
            <Harness controlType="TEXT" isFormulaMode={false} onEditor={handleEditor} type="STRING" value="" />
        );

        await settle();

        act(() => {
            editor!.commands.focus('end');
            editor!.commands.insertContent('hello');
        });

        act(() => {
            editor!.commands.blur();
        });

        view.rerender(
            <Harness
                controlType="TEXT"
                focusRequest={{token: 1}}
                isFormulaMode
                onEditor={handleEditor}
                type="STRING"
                value="'hello'"
            />
        );

        await waitPastSaveDebounce();

        expect(savedValues()).not.toContain('=hello');
        expect(savedValues()).not.toContain('hello');
    });

    it('is not flushed when the editor unmounts after the hook cancels it', async () => {
        let editor: Editor | null = null;

        const cancelPendingSaveRef: MutableRefObject<(() => void) | null> = {current: null};

        const handleEditor = (instance: Editor | null) => {
            editor = instance;
        };

        const view = render(
            <Harness
                cancelPendingSaveRef={cancelPendingSaveRef}
                controlType="INTEGER"
                isFormulaMode
                onEditor={handleEditor}
                type="INTEGER"
                value="5"
            />
        );

        await settle();

        act(() => {
            editor!.commands.focus('end');
            editor!.commands.insertContent('7');
        });

        act(() => {
            cancelPendingSaveRef.current?.();
        });

        view.unmount();

        await waitPastSaveDebounce();

        expect(savedValues()).not.toContain('=57');
        expect(cancelPendingSaveRef.current).toBeNull();
    });
});
