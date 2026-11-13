vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {UseFormReturn, useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const FROM_AI_VALUE = "=fromAi('bodyContentType', 'STRING', {'options': {'JSON', 'XML'}})";

const bodyContentTypeProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Body Content Type',
    name: 'bodyContentType',
    options: [
        {label: 'JSON', value: 'JSON'},
        {label: 'XML', value: 'XML'},
    ],
    type: 'STRING',
} as PropertyAllType;

const bodyProperty = {
    controlType: 'OBJECT_BUILDER',
    label: 'Body',
    name: 'body',
    properties: [bodyContentTypeProperty],
    type: 'OBJECT',
} as unknown as PropertyAllType;

let formRef: UseFormReturn<Record<string, unknown>> | undefined;

const ControlledWrapper = ({
    defaultValues,
    property,
    toolsMode = true,
}: {
    defaultValues: Record<string, unknown>;
    property: PropertyAllType;
    toolsMode?: boolean;
}) => {
    const form = useForm<Record<string, unknown>>({defaultValues});

    formRef = form;

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property
                    control={form.control as never}
                    controlPath=""
                    formState={form.formState as never}
                    property={property}
                    toolsMode={toolsMode}
                />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const expectFromAiRendering = async () => {
    await waitFor(() => expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument());

    expect(screen.queryByText('Select...')).toBeNull();
};

describe('controlled tool SELECT holding a fromAi value', () => {
    beforeEach(() => {
        formRef = undefined;

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-select-from-ai', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, pillTarget: null});
    });

    it.each([
        ['top level', bodyContentTypeProperty, {bodyContentType: FROM_AI_VALUE}],
        ['nested in an object', bodyProperty, {body: {bodyContentType: FROM_AI_VALUE}}],
    ])('renders the AI state for a saved value at %s', async (_case, property, defaultValues) => {
        render(<ControlledWrapper defaultValues={defaultValues} property={property} />);

        await expectFromAiRendering();
    });

    it.each([
        ['top level', bodyContentTypeProperty, {}, 'bodyContentType'],
        ['nested in an object', bodyProperty, {body: {}}, 'body.bodyContentType'],
    ])('renders the AI state for a value seeded after mount at %s', async (_case, property, defaultValues, path) => {
        render(<ControlledWrapper defaultValues={defaultValues} property={property} />);

        await waitFor(() => expect(screen.getByText('Select...')).toBeInTheDocument());

        act(() => formRef!.setValue(path, FROM_AI_VALUE));

        await expectFromAiRendering();
    });

    it('returns to the combobox when AI is toggled off', async () => {
        const {container} = render(
            <ControlledWrapper defaultValues={{bodyContentType: FROM_AI_VALUE}} property={bodyContentTypeProperty} />
        );

        await expectFromAiRendering();

        const fromAiButton = container.querySelector('.lucide-x')?.closest('button');

        expect(fromAiButton).toBeTruthy();

        fireEvent.click(fromAiButton!);

        await waitFor(() => expect(screen.getByText('Select...')).toBeInTheDocument());

        expect(screen.queryByText('Automatically defined by the model')).toBeNull();
        expect(formRef!.getValues('bodyContentType')).toBe('');
    });

    it('restores the previous value when AI is turned on and then off from the combobox', async () => {
        const {container} = render(
            <ControlledWrapper defaultValues={{bodyContentType: 'XML'}} property={bodyContentTypeProperty} />
        );

        await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());

        fireEvent.click(container.querySelector('.lucide-sparkles')!.closest('button')!);

        await expectFromAiRendering();

        fireEvent.click(container.querySelector('.lucide-x')!.closest('button')!);

        await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());

        expect(screen.queryByText('Automatically defined by the model')).toBeNull();
        expect(screen.queryByRole('switch', {checked: true})).toBeNull();
        expect(formRef!.getValues('bodyContentType')).toBe('XML');
    });

    it('falls back to the default value when nothing was held before AI was turned on', async () => {
        const {container} = render(
            <ControlledWrapper
                defaultValues={{bodyContentType: FROM_AI_VALUE}}
                property={{...bodyContentTypeProperty, defaultValue: 'JSON'} as unknown as PropertyAllType}
            />
        );

        await expectFromAiRendering();

        fireEvent.click(container.querySelector('.lucide-x')!.closest('button')!);

        await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());

        expect(formRef!.getValues('bodyContentType')).toBe('JSON');
    });

    it('keeps formula mode when AI is turned off on a text field', async () => {
        const {container} = render(
            <ControlledWrapper
                defaultValues={{title: 'hello'}}
                property={
                    {
                        controlType: 'TEXT',
                        expressionEnabled: true,
                        label: 'Title',
                        name: 'title',
                        type: 'STRING',
                    } as PropertyAllType
                }
            />
        );

        fireEvent.click(container.querySelector('.lucide-sparkles')!.closest('button')!);

        await expectFromAiRendering();

        fireEvent.click(container.querySelector('.lucide-x')!.closest('button')!);

        await waitFor(() => expect(screen.queryByText('Automatically defined by the model')).toBeNull());

        expect(String(formRef!.getValues('title'))).toContain('fromAi(');
    });

    it('keeps the combobox for a controlled field outside tools mode', async () => {
        render(
            <ControlledWrapper
                defaultValues={{bodyContentType: FROM_AI_VALUE}}
                property={bodyContentTypeProperty}
                toolsMode={false}
            />
        );

        await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());

        expect(screen.queryByText('Automatically defined by the model')).toBeNull();
    });
});
