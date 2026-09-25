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

    it('focusing the add item button does not make the array the pill target', async () => {
        renderUncontrolled(arrayProperty);

        await settle();

        const previousTarget = {acceptsPill: () => true, insertPill: vi.fn(), owner: 'previous-field'};

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: previousTarget});

        act(() => screen.getByRole('button', {name: /add array item/i}).focus());

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).toBe(previousTarget);
    });

    it('a controlled array does not become the pill target when its add item button is focused', async () => {
        render(<ControlledArrayWrapper />);

        await settle();

        const previousTarget = {acceptsPill: () => true, insertPill: vi.fn(), owner: 'previous-field'};

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: previousTarget});

        act(() => screen.getByRole('button', {name: /add item/i}).focus());

        await waitFor(() => expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).toBe(previousTarget));
    });
});
