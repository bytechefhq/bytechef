vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {fireEvent, waitFor} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * A controlled tool property switches between a plain input and the expression editor purely on the shape of
 * its form value, so the two swap places mid-edit. Both directions of that swap were broken: typing "=" left
 * the caret on the element being torn down, and emptying the editor could not clear the "=" that kept the
 * editor mounted, so the field was stuck in expression mode.
 */

const uriProperty = {
    controlType: 'TEXT',
    description: 'The URI to call',
    expressionEnabled: true,
    label: 'URI',
    name: 'uri',
    type: 'STRING',
} as PropertyAllType;

let formValues: Record<string, unknown> = {};

const Wrapper = ({uri}: {uri: string}) => {
    const form = useForm({defaultValues: {uri}});

    formValues = form.watch();

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property
                    control={form.control as never}
                    controlPath=""
                    formState={form.formState}
                    property={uriProperty}
                    toolsMode
                />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const microtaskTick = async (times = 1) => {
    for (let index = 0; index < times; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }
};

// The caret is handed over on a short timer, so the deferred focus has to be given a chance to run.
const settle = async () => {
    await microtaskTick(4);
    await new Promise<void>((resolve) => setTimeout(resolve, 120));
    await microtaskTick(2);
};

describe('controlled expression mode', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    beforeEach(() => {
        formValues = {};

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-controlled-expression', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: undefined,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    // Both controls report role "textbox" under the same label, so they are told apart by their element.
    const getEditor = (container: HTMLElement) => container.querySelector('.ProseMirror');
    const getInput = (container: HTMLElement) => container.querySelector('input');

    // A value that already starts with "=" when the field mounts opens in dynamic mode. Reaching expression
    // mode means typing the "=" into the plain input, which is the path that swaps the controls mid-edit.
    const typeEquals = (container: HTMLElement) => fireEvent.change(getInput(container)!, {target: {value: '='}});

    it('renders a plain input for a constant value', async () => {
        const {container} = render(<Wrapper uri="https://example.com" />);

        await settle();

        expect(getInput(container)).not.toBeNull();
        expect(getEditor(container)).toBeNull();
    });

    it('swaps in the expression editor when an = is typed', async () => {
        const {container} = render(<Wrapper uri="" />);

        await settle();

        typeEquals(container);

        await waitFor(() => expect(getEditor(container)).not.toBeNull());

        expect(getInput(container)).toBeNull();
    });

    // The editor replaces the input the user was typing into, so the caret has to be handed over. jsdom does
    // not move focus onto a contenteditable, so what this asserts is that the editor's node is asked for it.
    it('hands the caret to the editor when the field is switched over', async () => {
        const {container} = render(<Wrapper uri="" />);

        await settle();

        const focusSpy = vi.spyOn(HTMLElement.prototype, 'focus');

        typeEquals(container);

        await settle();

        expect(focusSpy.mock.instances).toContain(getEditor(container));
    });

    // A saved expression is opened for reading, not resumed mid-keystroke, so it must not steal the caret.
    it('does not steal the caret when a saved expression is reopened', async () => {
        const focusSpy = vi.spyOn(HTMLElement.prototype, 'focus');

        const {container} = render(<Wrapper uri="=concat('a', 'b')" />);

        await settle();

        expect(focusSpy.mock.instances).not.toContain(getEditor(container));
    });

    it('hands the caret back to the plain input when the editor is emptied', async () => {
        const {container} = render(<Wrapper uri="" />);

        await settle();

        typeEquals(container);

        await settle();

        fireEvent.keyDown(getEditor(container)!, {charCode: 8, code: 'Backspace', key: 'Backspace', keyCode: 8});

        await waitFor(() => expect(getInput(container)).not.toBeNull());

        expect(document.activeElement).toBe(getInput(container));
    });

    it('does not steal the caret from a constant value on first render', async () => {
        const {container} = render(<Wrapper uri="https://example.com" />);

        await settle();

        expect(document.activeElement).not.toBe(getInput(container));
    });

    // The value is what keeps the editor mounted, so leaving expression mode means clearing it. An already
    // empty editor emits no update, so nothing else was ever going to clear the stranded "=".
    it('clears the value when the editor is emptied, returning to the plain input', async () => {
        const {container} = render(<Wrapper uri="" />);

        await settle();

        typeEquals(container);

        await settle();

        fireEvent.keyDown(getEditor(container)!, {charCode: 8, code: 'Backspace', key: 'Backspace', keyCode: 8});

        await waitFor(() => expect(formValues.uri).toBe(''));

        expect(getEditor(container)).toBeNull();
        expect(getInput(container)).not.toBeNull();
    });
});
