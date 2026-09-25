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
import {act, fireEvent, screen} from '@testing-library/react';
import {FormProvider, useForm} from 'react-hook-form';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const textProperty = {
    controlType: 'TEXT',
    expressionEnabled: true,
    label: 'Text',
    name: 'text',
    type: 'STRING',
} as PropertyAllType;

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const objectProperty = {
    controlType: 'OBJECT_BUILDER',
    expressionEnabled: true,
    label: 'Settings',
    name: 'settings',
    properties: [textProperty, countProperty],
    type: 'OBJECT',
} as PropertyAllType;

const arrayProperty = {
    controlType: 'ARRAY_BUILDER',
    expressionEnabled: true,
    items: [countProperty],
    label: 'Counts',
    name: 'counts',
    type: 'ARRAY',
} as PropertyAllType;

const schemaProperty = {
    controlType: 'JSON_SCHEMA_BUILDER',
    expressionEnabled: true,
    label: 'Response',
    name: 'schema',
    type: 'STRING',
} as PropertyAllType;

const customObjectProperty = {
    controlType: 'OBJECT_BUILDER',
    expressionEnabled: true,
    label: 'Headers',
    name: 'headers',
    type: 'OBJECT',
} as PropertyAllType;

const dataPillTransfer = (mentionId: string) => ({
    getData: () => JSON.stringify({mentionId}),
    types: ['application/bytechef-datapill'],
});

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

const renderUncontrolled = (property: PropertyAllType) =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property path={property.name} property={property} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

const ControlledArrayWrapper = () => {
    const form = useForm({defaultValues: {counts: [5]}});

    return (
        <FormProvider {...form}>
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property
                        control={form.control as never}
                        controlPath=""
                        formState={form.formState}
                        property={arrayProperty}
                        toolsMode
                    />
                </WorkflowEditorProvider>
            </TooltipProvider>
        </FormProvider>
    );
};

const clickPill = (mentionId: string) => {
    const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

    if (pillTarget?.acceptsPill()) {
        act(() => pillTarget.insertPill(mentionId));
    }
};

const setParameters = (parameters: Record<string, unknown>) =>
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {name: 'node_1', parameters, workflowNodeName: 'node_1'},
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

const savedPaths = () => (saveProperty as unknown as Mock).mock.calls.map(([request]) => request.path);

describe('pill target inside an object or array builder', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-nested-pill', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                name: 'node_1',
                parameters: {counts: [5], settings: {count: 5, text: 'hello'}},
                workflowNodeName: 'node_1',
            },
            pillTarget: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('a pill clicked after focusing a nested text editor lands in that field, not over the object', async () => {
        const {container} = renderUncontrolled(objectProperty);

        await settle();

        const editorElement = container.querySelector('.ProseMirror') as HTMLElement;

        expect(editorElement).not.toBeNull();

        act(() => editorElement.focus());

        (saveProperty as unknown as Mock).mockClear();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('trigger_1.name'));

        await settle();

        expect(editorElement.textContent).toContain('trigger_1.name');
        expect(savedPaths()).not.toContain('settings');
    });

    it('a pill clicked after focusing a nested number field saves only that field', async () => {
        const {container} = renderUncontrolled(objectProperty);

        await settle();

        act(() => (container.querySelector('input[type=number]') as HTMLInputElement).focus());

        (saveProperty as unknown as Mock).mockClear();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('trigger_1.count'));

        await settle();

        expect(savedPaths()).toEqual(['settings.count']);
        expect(saveProperty).toHaveBeenCalledWith(
            expect.objectContaining({path: 'settings.count', value: '${trigger_1.count}'})
        );
    });

    it('a pill dropped on a nested number field is handled once, by that field', async () => {
        const {container} = renderUncontrolled(objectProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        fireEvent.drop(container.querySelector('input[type=number]')!, {
            dataTransfer: dataPillTransfer('trigger_1.count'),
        });

        await settle();

        expect(savedPaths()).toEqual(['settings.count']);
    });

    it('a pill dropped on a nested array item is handled once, by that item', async () => {
        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        fireEvent.drop(container.querySelector('input[type=number]')!, {
            dataTransfer: dataPillTransfer('trigger_1.count'),
        });

        await settle();

        expect(savedPaths()).toEqual(['counts[0]']);
    });

    it('focusing the add item button of a non-empty array replaces the previous target with one that rejects pills', async () => {
        renderUncontrolled(arrayProperty);

        await settle();

        const previousTarget = {acceptsPill: () => true, insertPill: vi.fn(), owner: 'previous-field'};

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: previousTarget});

        act(() => screen.getByRole('button', {name: /add array item/i}).focus());

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        expect(pillTarget).not.toBe(previousTarget);
        expect(pillTarget === null || pillTarget.acceptsPill() === false).toBe(true);
    });

    it('focusing inside a JSON schema builder does not let a pill replace the whole schema', async () => {
        const schemaValue = JSON.stringify({properties: {name: {type: 'string'}}, type: 'object'});

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {schema: schemaValue}, workflowNodeName: 'node_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

        renderUncontrolled(schemaProperty);

        await settle();

        const previousTarget = {acceptsPill: () => true, insertPill: vi.fn(), owner: 'previous-field'};

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: previousTarget});

        act(() => screen.getByRole('button', {name: /open response template/i}).focus());

        (saveProperty as unknown as Mock).mockClear();

        clickPill('trigger_1.name');

        await settle();

        expect(savedPaths()).not.toContain('schema');
        expect(previousTarget.insertPill).not.toHaveBeenCalled();
    });

    it('a pill clicked after focusing the add item button of an empty array becomes the whole array value', async () => {
        setParameters({counts: []});

        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        act(() => screen.getByRole('button', {name: /add array item/i}).focus());

        (saveProperty as unknown as Mock).mockClear();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('trigger_1.items'));

        await settle();

        expect(saveProperty).toHaveBeenCalledWith(
            expect.objectContaining({path: 'counts', value: '${trigger_1.items}'})
        );
        expect(container.querySelector('.ProseMirror')?.textContent).toContain('trigger_1.items');
    });

    it('a pill dropped on an empty array becomes the whole array value', async () => {
        setParameters({counts: []});

        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        fireEvent.drop(screen.getByRole('button', {name: /add array item/i}), {
            dataTransfer: dataPillTransfer('trigger_1.items'),
        });

        await settle();

        expect(savedPaths()).toEqual(['counts']);
        expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({value: '${trigger_1.items}'}));
        expect(container.querySelector('.ProseMirror')).not.toBeNull();
    });

    it('a pill clicked after focusing the add property button of an empty object becomes the whole object value', async () => {
        setParameters({headers: {}});

        const {container} = renderUncontrolled(customObjectProperty);

        await settle();

        act(() => screen.getByRole('button', {name: /add .*object property/i}).focus());

        (saveProperty as unknown as Mock).mockClear();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('trigger_1.headers'));

        await settle();

        expect(saveProperty).toHaveBeenCalledWith(
            expect.objectContaining({path: 'headers', value: '${trigger_1.headers}'})
        );
        expect(container.querySelector('.ProseMirror')).not.toBeNull();
    });

    it('a pill dropped on an empty object becomes the whole object value', async () => {
        setParameters({headers: {}});

        renderUncontrolled(customObjectProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        fireEvent.drop(screen.getByRole('button', {name: /add .*object property/i}), {
            dataTransfer: dataPillTransfer('trigger_1.headers'),
        });

        await settle();

        expect(savedPaths()).toEqual(['headers']);
    });

    it('a pill clicked after focusing a nested field of an empty object still lands in that field', async () => {
        setParameters({settings: {count: null, text: ''}});

        const {container} = renderUncontrolled(objectProperty);

        await settle();

        act(() => (container.querySelector('input[type=number]') as HTMLInputElement).focus());

        (saveProperty as unknown as Mock).mockClear();

        act(() => useWorkflowNodeDetailsPanelStore.getState().pillTarget!.insertPill('trigger_1.count'));

        await settle();

        expect(savedPaths()).toEqual(['settings.count']);
    });

    it('a non-empty array keeps its value when a pill is clicked or dropped after its add item button', async () => {
        renderUncontrolled(arrayProperty);

        await settle();

        const addItemButton = screen.getByRole('button', {name: /add array item/i});

        act(() => addItemButton.focus());

        (saveProperty as unknown as Mock).mockClear();

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        if (pillTarget?.acceptsPill()) {
            act(() => pillTarget.insertPill('trigger_1.items'));
        }

        fireEvent.drop(addItemButton, {dataTransfer: dataPillTransfer('trigger_1.items')});

        await settle();

        expect(savedPaths()).not.toContain('counts');
    });

    it('a pill clicked while the item just added to an empty array is still saving keeps the item', async () => {
        setParameters({counts: []});

        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        const addItemButton = screen.getByRole('button', {name: /add array item/i});

        act(() => addItemButton.focus());

        fireEvent.click(addItemButton);

        await settle();

        expect(container.querySelector('input[type=number]')).not.toBeNull();

        (saveProperty as unknown as Mock).mockClear();

        clickPill('trigger_1.items');

        fireEvent.drop(addItemButton, {dataTransfer: dataPillTransfer('trigger_1.items')});

        await settle();

        expect(saveProperty).not.toHaveBeenCalledWith(expect.objectContaining({value: '${trigger_1.items}'}));
        expect(container.querySelector('input[type=number]')).not.toBeNull();
        expect(container.querySelector('.ProseMirror')).toBeNull();
    });

    it('focusing the add item button of a non-empty array stops a pill from landing in the field focused before', async () => {
        const scalarProperty = {...countProperty, label: 'Limit', name: 'limit'} as PropertyAllType;

        setParameters({counts: [5], limit: 3});

        const {container} = render(
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property parameterValue={3} path="limit" property={scalarProperty} />

                    <Property path="counts" property={arrayProperty} />
                </WorkflowEditorProvider>
            </TooltipProvider>
        );

        await settle();

        act(() => (container.querySelector('[aria-label="limit property"] input') as HTMLInputElement).focus());

        act(() => screen.getByRole('button', {name: /add array item/i}).focus());

        (saveProperty as unknown as Mock).mockClear();

        clickPill('trigger_1.count');

        await settle();

        expect(savedPaths()).toEqual([]);
    });

    it('deleting the pill that holds a whole empty array brings the empty builder back', async () => {
        setParameters({counts: []});

        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        act(() => screen.getByRole('button', {name: /add array item/i}).focus());

        clickPill('trigger_1.items');

        await settle();

        const editorElement = container.querySelector('.ProseMirror') as HTMLElement & {
            editor: {commands: {clearContent: (emitUpdate: boolean) => void}};
        };

        expect(editorElement).not.toBeNull();

        act(() => editorElement.focus());
        act(() => editorElement.editor.commands.clearContent(true));
        act(() => editorElement.blur());

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(screen.getByRole('button', {name: /add array item/i})).toBeInTheDocument();
    });

    it('clicking the label of an empty array picks it as the pill target without adding an item', async () => {
        setParameters({counts: []});

        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        const labelElement = screen.getByText('Counts');

        fireEvent.mouseDown(labelElement);
        fireEvent.click(labelElement);

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.acceptsPill()).toBe(true);
        expect(screen.getByText('Drop or click a data pill')).toHaveClass('ring-2');

        (saveProperty as unknown as Mock).mockClear();

        clickPill('trigger_1.items');

        await settle();

        expect(savedPaths()).toEqual(['counts']);
        expect(saveProperty).toHaveBeenCalledWith(expect.objectContaining({value: '${trigger_1.items}'}));
        expect(container.querySelector('input[type=number]')).toBeNull();
        expect(screen.queryByText('Drop or click a data pill')).toBeNull();
    });

    it('clicking the add item button of an empty array adds an item and leaves no accepting container target', async () => {
        setParameters({counts: []});

        const {container} = renderUncontrolled(arrayProperty);

        await settle();

        const addItemButton = screen.getByRole('button', {name: /add array item/i});

        fireEvent.mouseDown(addItemButton);
        act(() => addItemButton.focus());
        fireEvent.click(addItemButton);

        await settle();

        expect(container.querySelector('input[type=number]')).not.toBeNull();
        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.acceptsPill() ?? false).toBe(false);
    });

    it('shows the data pill hint on an empty array and hides it once an item exists', async () => {
        setParameters({counts: []});

        renderUncontrolled(arrayProperty);

        await settle();

        expect(screen.getByText('Drop or click a data pill')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: /add array item/i}));

        await settle();

        expect(screen.queryByText('Drop or click a data pill')).toBeNull();
    });

    it('does not show the data pill hint on a non-empty array', async () => {
        renderUncontrolled(arrayProperty);

        await settle();

        expect(screen.queryByText('Drop or click a data pill')).toBeNull();
    });

    it('a click on a nested field of a non-empty object registers the nested field, not the object', async () => {
        const {container} = renderUncontrolled(objectProperty);

        await settle();

        const nestedInput = container.querySelector('input[type=number]') as HTMLInputElement;

        fireEvent.mouseDown(nestedInput);
        act(() => nestedInput.focus());
        fireEvent.click(nestedInput);

        (saveProperty as unknown as Mock).mockClear();

        clickPill('trigger_1.count');

        await settle();

        expect(savedPaths()).toEqual(['settings.count']);
    });

    it('a controlled array takes no pill, and no earlier field does, when its add item button is focused', async () => {
        render(<ControlledArrayWrapper />);

        await settle();

        const previousTarget = {acceptsPill: () => true, insertPill: vi.fn(), owner: 'previous-field'};

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: previousTarget});

        act(() => screen.getByRole('button', {name: /add item/i}).focus());

        clickPill('trigger_1.count');

        await settle();

        expect(previousTarget.insertPill).not.toHaveBeenCalled();
        expect(savedPaths()).toEqual([]);
    });
});
