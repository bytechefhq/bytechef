vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import {FormulaEnabledProvider} from '@/pages/platform/workflow-editor/components/properties/FormulaEnabledContext';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {ReactNode} from 'react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const uriProperty = {
    controlType: 'TEXT',
    expressionEnabled: true,
    label: 'URI',
    name: 'uri',
    type: 'STRING',
} as PropertyAllType;

type EditorElementType = HTMLElement & {editor: Editor};

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await act(async () => {
            await new Promise<void>((resolve) => setTimeout(resolve, 0));
        });
    }

    await act(async () => {
        await new Promise<void>((resolve) => setTimeout(resolve, 120));
    });
};

const renderProperty = (
    property: PropertyAllType,
    parameterValue?: unknown,
    {formulaEnabled}: {formulaEnabled?: boolean} = {}
) => {
    let content: ReactNode = (
        <Property parameterValue={parameterValue} path={`parameters.${property.name}`} property={property} />
    );

    if (formulaEnabled !== undefined) {
        content = <FormulaEnabledProvider value={formulaEnabled}>{content}</FormulaEnabledProvider>;
    }

    return render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>{content}</WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const savedValues = () => (saveProperty as unknown as Mock).mock.calls.map(([request]) => request.value);

const getEditorElement = (container: HTMLElement) => container.querySelector('.ProseMirror') as EditorElementType;

describe('uncontrolled Formula switch', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-uncontrolled-formula', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {}, workflowNodeName: 'node_1'},
            pillTarget: null,
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('converts a lone pill into a formula and back into the one-pill editor', async () => {
        const {container} = renderProperty(countProperty, '${trigger_1.count}');

        await settle();

        expect(container.querySelector('.ProseMirror [data-id="trigger_1.count"]')).not.toBeNull();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'true');
        expect(container.querySelector('.ProseMirror [data-id="trigger_1.count"]')).not.toBeNull();
        expect(savedValues()).toEqual(['=${trigger_1.count}']);

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');
        expect(container.querySelector('.ProseMirror [data-id="trigger_1.count"]')).not.toBeNull();
        expect(container.querySelector('input[type=number]')).toBeNull();
        expect(savedValues()).toEqual(['=${trigger_1.count}', '${trigger_1.count}']);
    });

    it('clears mixed text and pills when Formula turns on', async () => {
        const {container} = renderProperty(uriProperty, 'hello ${trigger_1.name}');

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'true');
        expect(getEditorElement(container).textContent).toBe('');
        expect(savedValues()).toEqual([null]);
    });

    it('focuses the formula editor at the end when Formula turns on', async () => {
        const {container} = renderProperty(countProperty, 5);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        const editorElement = getEditorElement(container);

        expect(editorElement.textContent).toBe('5');
        expect(document.activeElement).toBe(editorElement);
        expect(editorElement.editor.state.selection.to).toBe(editorElement.editor.state.doc.content.size - 1);
    });

    it('focuses the native input when Formula turns off', async () => {
        const {container} = renderProperty(countProperty, '=7');

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        const input = container.querySelector('input[type=number]');

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(input).not.toBeNull();
        expect(document.activeElement).toBe(input);
    });

    it('leaves Formula on Backspace in an empty formula and returns to the native input', async () => {
        const {container} = renderProperty(countProperty, '');

        await settle();

        const input = container.querySelector('input[type=number]') as HTMLInputElement;

        fireEvent.focus(input);
        fireEvent.keyDown(input, {key: '='});

        await settle();

        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'true');

        fireEvent.keyDown(getEditorElement(container), {
            charCode: 8,
            code: 'Backspace',
            key: 'Backspace',
            keyCode: 8,
        });

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(container.querySelector('input[type=number]')).not.toBeNull();
        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');
    });

    it('brings back the focused native input when the pill is deleted', async () => {
        const {container} = renderProperty(countProperty, '${trigger_1.count}');

        await settle();

        const editorElement = getEditorElement(container);

        act(() => {
            editorElement.editor.commands.focus('end');
        });
        act(() => {
            editorElement.editor.commands.clearContent(true);
        });

        await settle();

        const input = container.querySelector('input[type=number]');

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(input).not.toBeNull();
        expect(document.activeElement).toBe(input);
    });

    it('keeps a legacy value with text around a pill in the editor and adds a clicked pill next to it', async () => {
        const {container} = renderProperty(countProperty, '${trigger_1.count} ms');

        await settle();

        const editorElement = getEditorElement(container);

        expect(editorElement.textContent).toContain(' ms');
        expect(editorElement.querySelectorAll('.property-mention')).toHaveLength(1);

        act(() => {
            editorElement.editor.commands.focus('end');
        });

        await settle();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('trigger_1.total'));

        await settle();

        expect(editorElement.querySelector('[data-id="trigger_1.count"]')).not.toBeNull();
        expect(editorElement.querySelector('[data-id="trigger_1.total"]')).not.toBeNull();
    });

    it.each([
        ['a STRING text field', uriProperty, 'https://example.com'],
        ['an INTEGER field', countProperty, 5],
        [
            'a BOOLEAN select',
            {controlType: 'SELECT', expressionEnabled: true, label: 'Enabled', name: 'enabled', type: 'BOOLEAN'},
            true,
        ],
        [
            'an options select',
            {
                controlType: 'SELECT',
                expressionEnabled: true,
                label: 'Method',
                name: 'method',
                options: [
                    {label: 'GET', value: 'GET'},
                    {label: 'POST', value: 'POST'},
                ],
                type: 'STRING',
            },
            'GET',
        ],
    ])('shows the Formula switch on %s', async (_, property, parameterValue) => {
        renderProperty(property as PropertyAllType, parameterValue);

        await settle();

        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');
    });

    it('never shows Formula for a fromAi value', async () => {
        renderProperty(countProperty, "=fromAi('count', 'INTEGER', {'required': false})");

        await settle();

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
    });

    it('hides the switch when the surface disables formulas', async () => {
        renderProperty(countProperty, 5, {formulaEnabled: false});

        await settle();

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
    });

    it('hides the switch when expressions are disabled', async () => {
        renderProperty({...countProperty, expressionEnabled: false} as PropertyAllType, 5);

        await settle();

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
    });
});
