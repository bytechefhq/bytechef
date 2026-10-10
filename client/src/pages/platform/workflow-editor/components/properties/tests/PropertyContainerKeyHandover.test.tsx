vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const arrayProperty = {
    controlType: 'ARRAY_BUILDER',
    expressionEnabled: true,
    items: [countProperty],
    label: 'Counts',
    name: 'counts',
    type: 'ARRAY',
} as PropertyAllType;

const objectProperty = {
    controlType: 'OBJECT_BUILDER',
    expressionEnabled: true,
    label: 'Settings',
    name: 'settings',
    properties: [countProperty],
    type: 'OBJECT',
} as PropertyAllType;

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

const renderProperty = (property: PropertyAllType) =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property path={property.name} property={property} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

const setParameters = (parameters: Record<string, unknown>) =>
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {name: 'node_1', parameters, workflowNodeName: 'node_1'},
        pillTarget: null,
        workflowNodeDetailsPanelOpen: true,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

const pressKeyOn = (element: HTMLElement, key: string) => {
    act(() => element.focus());

    fireEvent.keyDown(element, {key});
};

describe('keyboard handover from an empty container', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-container-key-handover', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);
    });

    it('the Dynamic switch of an empty array swaps to an empty one-pill editor', async () => {
        setParameters({counts: []});

        const {container} = renderProperty(arrayProperty);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Dynamic'}));

        await settle();

        const editorElement = container.querySelector('.ProseMirror');

        expect(editorElement).not.toBeNull();
        expect(editorElement!.textContent).toBe('');
        expect(document.activeElement).toBe(editorElement);
        expect(screen.getByRole('switch', {name: 'Dynamic'})).toBeChecked();
        expect(screen.queryByRole('button', {name: /add array item/i})).toBeNull();
    });

    it('$ on the add item button of an empty array swaps to the one-pill editor', async () => {
        setParameters({counts: []});

        const {container} = renderProperty(arrayProperty);

        await settle();

        pressKeyOn(screen.getByRole('button', {name: /add array item/i}), '$');

        await settle();

        expect(container.querySelector('.ProseMirror')?.textContent).toBe('$');
    });

    it('= on the add item button of an empty array swaps to the formula editor', async () => {
        setParameters({counts: []});

        const {container} = renderProperty(arrayProperty);

        await settle();

        pressKeyOn(screen.getByRole('button', {name: /add array item/i}), '=');

        await settle();

        const editorElement = container.querySelector('.ProseMirror');

        expect(editorElement).not.toBeNull();
        expect(editorElement!.textContent).not.toBe('$');
        expect(document.activeElement).toBe(editorElement);
    });

    it('ignores $ on the add item button of a non-empty array', async () => {
        setParameters({counts: [5]});

        const {container} = renderProperty(arrayProperty);

        await settle();

        const addItemButton = screen.getByRole('button', {name: /add array item/i});

        pressKeyOn(addItemButton, '$');

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(screen.getByRole('button', {name: /add array item/i})).toBeInTheDocument();
    });

    it('$ in a nested field of an empty object stays with that field', async () => {
        setParameters({settings: {}});

        const {container} = renderProperty(objectProperty);

        await settle();

        const nestedInput = container.querySelector('input[type=number]') as HTMLInputElement;

        pressKeyOn(nestedInput, '$');

        await settle();

        expect(container.querySelector('input[type=number]')).toBeNull();
        expect(screen.getByRole('switch', {name: 'Dynamic'})).not.toBeChecked();
    });
});
