vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {StrictMode} from 'react';
import {useForm} from 'react-hook-form';
import {type Mock, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const originalAppend = Element.prototype.append;

const blurFocusedElementWhenMoved = function (this: Element, ...nodes: Array<Node | string>) {
    const focusedElement = document.activeElement;

    const movesFocusedElement = nodes.some(
        (node) => typeof node !== 'string' && focusedElement instanceof HTMLElement && node.contains(focusedElement)
    );

    if (movesFocusedElement) {
        (focusedElement as HTMLElement).blur();
    }

    originalAppend.apply(this, nodes);
};

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

const renderEmptyCount = () =>
    render(
        <StrictMode>
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property parameterValue="" path="parameters.count" property={countProperty} />
                </WorkflowEditorProvider>
            </TooltipProvider>
        </StrictMode>
    );

describe('keyboard handover from an empty number field', () => {
    beforeEach(() => {
        Element.prototype.append = blurFocusedElementWhenMoved;

        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-key-handover', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {}, workflowNodeName: 'node_1'},
            pillTarget: null,
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    afterEach(() => {
        Element.prototype.append = originalAppend;
    });

    it('$ swaps to the one-pill editor with $ typed, and the editor stays', async () => {
        const {container} = renderEmptyCount();

        const input = container.querySelector('input[type=number]') as HTMLInputElement;

        fireEvent.focus(input);
        fireEvent.keyDown(input, {key: '$'});

        await settle();

        const editorElement = container.querySelector('.ProseMirror');

        expect(editorElement).not.toBeNull();
        expect(container.querySelector('input[type=number]')).toBeNull();
        expect(editorElement!.textContent).toBe('$');
        expect(document.activeElement).toBe(editorElement);
    });

    it('= swaps to the formula editor, and the editor stays', async () => {
        const {container} = renderEmptyCount();

        const input = container.querySelector('input[type=number]') as HTMLInputElement;

        fireEvent.focus(input);
        fireEvent.keyDown(input, {key: '='});

        await settle();

        const editorElement = container.querySelector('.ProseMirror');

        expect(editorElement).not.toBeNull();
        expect(container.querySelector('input[type=number]')).toBeNull();
        expect(document.activeElement).toBe(editorElement);
    });

    it.each(['$', '='])('%s on a number field holding a typed value keeps the native input', async (key) => {
        const {container} = renderEmptyCount();

        const input = container.querySelector('input[type=number]') as HTMLInputElement;

        fireEvent.focus(input);
        fireEvent.change(input, {target: {value: '5'}});
        fireEvent.keyDown(input, {key});

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(container.querySelector('input[type=number]')).not.toBeNull();
        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');
    });
});

const methodProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Method',
    name: 'method',
    options: [
        {label: 'GET', value: 'GET'},
        {label: 'POST', value: 'POST'},
    ],
    type: 'STRING',
} as PropertyAllType;

const enabledProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Enabled',
    name: 'enabled',
    type: 'BOOLEAN',
} as PropertyAllType;

const renderSelect = (property: PropertyAllType, parameterValue: unknown) =>
    render(
        <StrictMode>
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property
                        parameterValue={parameterValue}
                        path={`parameters.${property.name}`}
                        property={property}
                    />
                </WorkflowEditorProvider>
            </TooltipProvider>
        </StrictMode>
    );

describe('keyboard handover from a select holding an option', () => {
    beforeEach(() => {
        Element.prototype.append = blurFocusedElementWhenMoved;

        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-key-handover', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {}, workflowNodeName: 'node_1'},
            pillTarget: null,
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    afterEach(() => {
        Element.prototype.append = originalAppend;
    });

    it.each([
        ['an options combobox', methodProperty, 'GET', 'GET'],
        ['a BOOLEAN select', enabledProperty, true, 'True'],
    ])('$ on %s swaps to an empty one-pill editor with $ typed', async (_, property, parameterValue, optionLabel) => {
        const {container} = renderSelect(property, parameterValue);

        const trigger = container.querySelector('[role=combobox]') as HTMLElement;

        expect(trigger.textContent).toContain(optionLabel);

        fireEvent.focus(trigger);
        fireEvent.keyDown(trigger, {key: '$'});

        await settle();

        const editorElement = container.querySelector('.ProseMirror');

        expect(editorElement).not.toBeNull();
        expect(container.querySelector('[role=combobox]')).toBeNull();
        expect(editorElement!.textContent).toBe('$');
        expect(document.activeElement).toBe(editorElement);
    });

    it.each([
        ['an options combobox', methodProperty, 'GET', 'GET'],
        ['a BOOLEAN select', enabledProperty, true, 'True'],
    ])(
        'leaving %s pill entry without a pill keeps the editor, and turning Dynamic off brings the option back',
        async (_, property, parameterValue, optionLabel) => {
            const {container} = renderSelect(property, parameterValue);

            const trigger = container.querySelector('[role=combobox]') as HTMLElement;

            fireEvent.focus(trigger);
            fireEvent.keyDown(trigger, {key: '$'});

            await settle();

            const editorElement = container.querySelector('.ProseMirror') as HTMLElement;

            act(() => editorElement.blur());

            await settle();

            expect(container.querySelector('.ProseMirror')).not.toBeNull();
            expect(screen.getByRole('switch', {name: 'Dynamic'})).toBeChecked();

            fireEvent.click(screen.getByRole('switch', {name: 'Dynamic'}));

            await settle();

            const restoredTrigger = container.querySelector('[role=combobox]');

            expect(container.querySelector('.ProseMirror')).toBeNull();
            expect(restoredTrigger?.textContent).toContain(optionLabel);
            expect(saveProperty).not.toHaveBeenCalled();
        }
    );

    it('= on a select holding an option keeps the select', async () => {
        const {container} = renderSelect(methodProperty, 'GET');

        const trigger = container.querySelector('[role=combobox]') as HTMLElement;

        fireEvent.focus(trigger);
        fireEvent.keyDown(trigger, {key: '='});

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(container.querySelector('[role=combobox]')?.textContent).toContain('GET');
        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');
        expect(saveProperty).not.toHaveBeenCalled();
    });
});

const tagsProperty = {
    controlType: 'ARRAY_BUILDER',
    expressionEnabled: true,
    label: 'Tags',
    name: 'tags',
    type: 'ARRAY',
} as PropertyAllType;

describe('the Dynamic switch', () => {
    beforeEach(() => {
        Element.prototype.append = blurFocusedElementWhenMoved;

        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-key-handover', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {}, workflowNodeName: 'node_1'},
            pillTarget: null,
            workflowNodeDetailsPanelOpen: true,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    afterEach(() => {
        Element.prototype.append = originalAppend;
    });

    it.each([
        ['an options combobox', methodProperty, 'GET'],
        ['a BOOLEAN select', enabledProperty, true],
        ['an empty array builder', tagsProperty, undefined],
    ])('on %s opens an empty editor', async (_, property, parameterValue) => {
        const {container} = renderSelect(property, parameterValue);

        const dynamicSwitch = screen.getByRole('switch', {name: 'Dynamic'});

        expect(dynamicSwitch).not.toBeChecked();
        expect(screen.queryByText('Drop or click a data pill, or type $')).toBeNull();

        fireEvent.click(dynamicSwitch);

        await settle();

        const editorElement = container.querySelector('.ProseMirror');

        expect(editorElement).not.toBeNull();
        expect(editorElement!.textContent).toBe('');
        expect(screen.getByRole('switch', {name: 'Dynamic'})).toBeChecked();
    });

    it('keeps the editor when focus moves to the data pill panel, and takes the pill picked there', async () => {
        const {container} = renderSelect(enabledProperty, true);

        fireEvent.click(screen.getByRole('switch', {name: 'Dynamic'}));

        await settle();

        act(() => (container.querySelector('.ProseMirror') as HTMLElement).blur());

        await settle();

        expect(container.querySelector('.ProseMirror')).not.toBeNull();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget?.insertPill('trigger_1.flag'));

        await waitFor(() =>
            expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({value: '${trigger_1.flag}'}))
        );

        expect(container.querySelector('.ProseMirror [data-id="trigger_1.flag"]')).not.toBeNull();
    });

    it.each([
        [
            'an empty object builder',
            {
                controlType: 'OBJECT_BUILDER',
                expressionEnabled: true,
                label: 'Settings',
                name: 'settings',
                properties: [countProperty],
                type: 'OBJECT',
            },
            {},
        ],
        [
            'an empty JSON schema builder',
            {
                controlType: 'JSON_SCHEMA_BUILDER',
                expressionEnabled: true,
                label: 'Response',
                name: 'schema',
                type: 'STRING',
            },
            '',
        ],
    ])('is offered on %s', (_, property, parameterValue) => {
        renderSelect(property as PropertyAllType, parameterValue);

        expect(screen.getByRole('switch', {name: 'Dynamic'})).not.toBeChecked();
    });

    it.each([
        ['a number field', countProperty, 5],
        [
            'a text field',
            {controlType: 'TEXT', expressionEnabled: true, label: 'URI', name: 'uri', type: 'STRING'},
            'x',
        ],
        [
            'an array item object',
            {
                controlType: 'OBJECT_BUILDER',
                expressionEnabled: true,
                label: 'Item',
                name: '__item',
                properties: [countProperty],
                type: 'OBJECT',
            },
            {},
        ],
        ['a select with expressions disabled', {...methodProperty, expressionEnabled: false}, 'GET'],
        ['an empty array builder with expressions disabled', {...tagsProperty, expressionEnabled: false}, undefined],
    ])('is not offered on %s', (_, property, parameterValue) => {
        renderSelect(property as PropertyAllType, parameterValue);

        expect(screen.queryByRole('switch', {name: 'Dynamic'})).toBeNull();
    });

    it('is not offered on a controlled select', () => {
        const ControlledSelect = () => {
            const form = useForm({defaultValues: {method: 'GET'}});

            return (
                <TooltipProvider>
                    <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                        <Property
                            control={form.control as never}
                            controlPath=""
                            formState={form.formState}
                            property={methodProperty}
                            toolsMode
                        />
                    </WorkflowEditorProvider>
                </TooltipProvider>
            );
        };

        render(<ControlledSelect />);

        expect(screen.queryByRole('switch', {name: 'Dynamic'})).toBeNull();
        expect(screen.getByRole('switch', {name: 'Formula'})).toBeInTheDocument();
    });

    it('is not offered in Formula mode', () => {
        renderSelect(methodProperty, "='GET'");

        expect(screen.queryByRole('switch', {name: 'Dynamic'})).toBeNull();
        expect(screen.getByRole('switch', {name: 'Formula'})).toBeChecked();
    });
});
