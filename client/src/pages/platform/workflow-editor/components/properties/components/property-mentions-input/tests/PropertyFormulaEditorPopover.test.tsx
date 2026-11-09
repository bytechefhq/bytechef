vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {ControlType} from '@/shared/middleware/platform/configuration';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';

interface RenderInputPropsI {
    controlType?: ControlType;
    isFormulaMode?: boolean;
    isFromAi?: boolean;
    setIsFormulaMode?: (isFormulaMode: boolean) => void;
    value?: string;
}

type EditorElementType = HTMLElement & {editor: Editor};

const WAIT_OPTIONS = {interval: 5, timeout: 5000};

const buildInput = ({
    controlType = 'TEXT',
    isFormulaMode = true,
    isFromAi = false,
    setIsFormulaMode = () => {},
    value = '',
}: RenderInputPropsI) => (
    <TooltipProvider>
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <div data-data-pill-panel="">
                <button type="button">Data pill</button>
            </div>

            <button type="button">Canvas</button>

            <PropertyMentionsInput
                controlType={controlType}
                isFormulaMode={isFormulaMode}
                isFromAi={isFromAi}
                label="Body"
                path="parameters.body"
                setIsFormulaMode={setIsFormulaMode}
                showInputTypeSwitchButton
                type="STRING"
                value={value}
            />
        </WorkflowEditorProvider>
    </TooltipProvider>
);

const renderInput = async (props: RenderInputPropsI) => {
    const result = render(buildInput(props));

    if (props.isFromAi) {
        await waitFor(() => expect(screen.getByText('Body')).toBeInTheDocument(), WAIT_OPTIONS);

        return {...result, inlineEditor: null};
    }

    await waitFor(() => expect(result.container.querySelector('.ProseMirror')).not.toBeNull(), WAIT_OPTIONS);

    const inlineEditorElement = result.container.querySelector('.ProseMirror') as EditorElementType;

    return {...result, inlineEditor: inlineEditorElement.editor};
};

const queryFormulaEditorButton = () => screen.queryByRole('button', {name: 'Open formula editor'});

const waitForFormulaEditorButton = () =>
    waitFor(() => expect(queryFormulaEditorButton()).toBeInTheDocument(), WAIT_OPTIONS);

const countExpandButtons = (container: HTMLElement) => container.querySelectorAll('.lucide-maximize').length;

const getPopoverEditor = () => {
    const popover = document.querySelector('[data-slot="popover-content"]');

    const editorElement = popover?.querySelector('.ProseMirror') as (HTMLElement & {editor?: Editor}) | null;

    return editorElement?.editor ?? null;
};

const openFormulaEditor = async () => {
    await waitForFormulaEditorButton();

    fireEvent.click(queryFormulaEditorButton()!);

    await waitFor(() => expect(getPopoverEditor()).not.toBeNull(), WAIT_OPTIONS);

    return getPopoverEditor()!;
};

const focusPopoverEditor = async (popoverEditor: Editor) => {
    act(() => {
        popoverEditor.commands.focus('end');
    });

    await waitFor(
        () => expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.owner).toBe(popoverEditor),
        WAIT_OPTIONS
    );
};

const expectLastSave = (value: string | null) =>
    waitFor(
        () => expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({path: 'parameters.body', value})),
        WAIT_OPTIONS
    );

describe('formula editor popover', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-formula-editor', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
            pillTarget: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('is offered only in formula mode', async () => {
        const {rerender} = await renderInput({isFormulaMode: false});

        expect(queryFormulaEditorButton()).toBeNull();

        rerender(buildInput({isFormulaMode: true}));

        await waitForFormulaEditorButton();
    });

    it('is not offered on a value defined by the model', async () => {
        await renderInput({isFromAi: true, value: "=fromAi('body', 'STRING')"});

        expect(queryFormulaEditorButton()).toBeNull();
    });

    it.each(['TEXT_AREA', 'RICH_TEXT'] as ControlType[])(
        'keeps a single expand button on a %s field, the side panel in text mode and the formula editor in formula mode',
        async (controlType) => {
            const {container, unmount} = await renderInput({controlType, isFormulaMode: false});

            await waitFor(() => expect(countExpandButtons(container)).toBe(1), WAIT_OPTIONS);

            expect(queryFormulaEditorButton()).toBeNull();

            unmount();

            const formulaRender = await renderInput({controlType, isFormulaMode: true});

            await waitForFormulaEditorButton();

            expect(countExpandButtons(formulaRender.container)).toBe(1);
        }
    );

    it('opens the formula in a formula mode editor', async () => {
        await renderInput({value: "=concat('a', 'b')"});

        const popoverEditor = await openFormulaEditor();

        expect(popoverEditor.state.doc.textContent).toBe("concat('a', 'b')");
        expect(
            document.querySelector('[data-slot="popover-content"] .property-mentions-editor--formula-mode')
        ).not.toBe(null);
        expect(screen.getAllByText('This field supports functions to build a value.').length).toBeGreaterThan(0);
    });

    it('saves what is typed in the popover as a formula', async () => {
        await renderInput({value: ''});

        const popoverEditor = await openFormulaEditor();

        act(() => {
            popoverEditor.commands.insertContent("concat('a', 'b')");
        });

        await expectLastSave("=concat('a', 'b')");
    });

    it('leaves formula mode when the emptied formula is erased again', async () => {
        const setIsFormulaMode = vi.fn();

        await renderInput({setIsFormulaMode, value: ''});

        const popoverEditor = await openFormulaEditor();

        await focusPopoverEditor(popoverEditor);

        fireEvent.keyDown(popoverEditor.view.dom, {key: 'Backspace'});

        await waitFor(() => expect(setIsFormulaMode).toHaveBeenCalledWith(false), WAIT_OPTIONS);
    });

    it('saves a value typed in the field again after the formula editor replaced it', async () => {
        const {inlineEditor: renderedInlineEditor, rerender} = await renderInput({value: ''});

        const inlineEditor = renderedInlineEditor!;

        act(() => {
            inlineEditor.commands.insertContent('a');
        });

        await expectLastSave('=a');

        rerender(buildInput({value: '=b'}));

        await waitFor(() => expect(inlineEditor.state.doc.textContent).toBe('b'), WAIT_OPTIONS);

        (saveProperty as unknown as Mock).mockClear();

        act(() => {
            inlineEditor.commands.setContent('a', {emitUpdate: true});
        });

        await expectLastSave('=a');
    });

    it('stays open while data pills are picked and closes once focus moves elsewhere', async () => {
        await renderInput({value: ''});

        await openFormulaEditor();

        act(() => {
            screen.getByRole('button', {name: 'Data pill'}).focus();
        });

        expect(getPopoverEditor()).not.toBeNull();

        act(() => {
            screen.getByRole('button', {name: 'Canvas'}).focus();
        });

        await waitFor(() => expect(getPopoverEditor()).toBeNull(), WAIT_OPTIONS);
    });

    it('closes a suggestion list on Escape without closing the formula editor', async () => {
        await renderInput({value: ''});

        const popoverEditor = await openFormulaEditor();

        popoverEditor.storage.MentionStorage.suggestionOpen = true;

        fireEvent.keyDown(document.body, {key: 'Escape'});

        expect(getPopoverEditor()).not.toBeNull();

        popoverEditor.storage.MentionStorage.suggestionOpen = false;

        fireEvent.keyDown(document.body, {key: 'Escape'});

        await waitFor(() => expect(getPopoverEditor()).toBeNull(), WAIT_OPTIONS);
    });

    it('closes with its close button', async () => {
        await renderInput({value: ''});

        await openFormulaEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Close formula editor'}));

        await waitFor(() => expect(getPopoverEditor()).toBeNull(), WAIT_OPTIONS);
    });

    it('takes data pills while focused and releases them once closed', async () => {
        await renderInput({value: ''});

        const popoverEditor = await openFormulaEditor();

        await focusPopoverEditor(popoverEditor);

        fireEvent.keyDown(document.activeElement ?? document.body, {key: 'Escape'});

        await waitFor(() => expect(getPopoverEditor()).toBeNull(), WAIT_OPTIONS);

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.owner).not.toBe(popoverEditor);
    });

    it('releases data pills when the formula editor goes away while open', async () => {
        const {rerender} = await renderInput({value: "=concat('a')"});

        const popoverEditor = await openFormulaEditor();

        await focusPopoverEditor(popoverEditor);

        rerender(buildInput({isFromAi: true, value: "=fromAi('body', 'STRING')"}));

        await waitFor(() => expect(queryFormulaEditorButton()).toBeNull(), WAIT_OPTIONS);

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.owner).not.toBe(popoverEditor);
    });
});
