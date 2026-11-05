vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));
vi.mock('@/pages/platform/workflow-editor/utils/deleteProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen, within} from '@testing-library/react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

interface SaveRequestI {
    includeInMetadata?: boolean;
    path: string;
    type: string;
    value?: unknown;
}

const formInputItemProperty = {
    controlType: 'OBJECT_BUILDER',
    expressionEnabled: true,
    properties: [
        {
            controlType: 'SELECT',
            expressionEnabled: true,
            label: 'Field Type',
            name: 'fieldType',
            options: [
                {label: 'Date Picker', value: 2},
                {label: 'Textarea', value: 5},
                {label: 'Input', value: 6},
            ],
            required: true,
            type: 'INTEGER',
        },
        {controlType: 'TEXT', expressionEnabled: true, label: 'Field Label', name: 'fieldLabel', type: 'STRING'},
        {controlType: 'TEXT', expressionEnabled: true, label: 'Field Name', name: 'fieldName', type: 'STRING'},
    ],
    type: 'OBJECT',
} as PropertyAllType;

const inputsProperty = {
    controlType: 'ARRAY_BUILDER',
    expressionEnabled: true,
    items: [formInputItemProperty],
    label: 'Form Inputs',
    name: 'inputs',
    type: 'ARRAY',
} as PropertyAllType;

const settingsProperty = {
    controlType: 'OBJECT_BUILDER',
    expressionEnabled: true,
    label: 'Settings',
    name: 'settings',
    properties: [
        {controlType: 'TEXT', expressionEnabled: true, label: 'Text', name: 'text', type: 'STRING'},
        {
            controlType: 'ARRAY_BUILDER',
            expressionEnabled: true,
            items: [{controlType: 'TEXT', expressionEnabled: true, type: 'STRING'}],
            label: 'Tags',
            name: 'tags',
            type: 'ARRAY',
        },
    ],
    type: 'OBJECT',
} as PropertyAllType;

const formInputs = [
    {fieldLabel: 'Investitor', fieldName: 'investitor', fieldType: 6, required: true},
    {fieldLabel: 'Lokacija', fieldName: 'lokacija', fieldType: 6, required: true},
    {fieldLabel: 'Opis objekta i zahtjeva', fieldName: 'opis', fieldType: 5, required: true},
    {fieldLabel: 'Željeni rok', fieldName: 'zeljeniRok', fieldType: 2, required: true},
];

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await act(async () => {
            await new Promise<void>((resolve) => setTimeout(resolve, 0));
        });
    }

    await act(async () => {
        await new Promise<void>((resolve) => setTimeout(resolve, 700));
    });
};

const setParameterAtPath = (parameters: Record<string, unknown>, path: string, value: unknown) => {
    const pathParts = path.match(/[^.[\]]+/g) ?? [];

    let container = parameters as Record<string, unknown>;

    pathParts.forEach((pathPart, pathPartIndex) => {
        if (pathPartIndex === pathParts.length - 1) {
            container[pathPart] = value;

            return;
        }

        if (container[pathPart] == null) {
            container[pathPart] = /^\d+$/.test(pathParts[pathPartIndex + 1]) ? [] : {};
        }

        container = container[pathPart] as Record<string, unknown>;
    });
};

const respondLikeServer = (request: SaveRequestI) => {
    const {currentNode} = useWorkflowNodeDetailsPanelStore.getState();

    const parameters = structuredClone(currentNode?.parameters ?? {}) as Record<string, unknown>;

    setParameterAtPath(parameters, request.path, request.value === undefined ? null : request.value);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {...currentNode, parameters},
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
};

const renderProperty = (property: PropertyAllType) =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property path={property.name} property={property} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

const saveRequests = (): SaveRequestI[] =>
    (saveProperty as unknown as Mock).mock.calls.map(([request]) => ({
        includeInMetadata: request.includeInMetadata,
        path: request.path,
        type: request.type,
        value: request.value,
    }));

const getFormulaSwitch = (container: HTMLElement) => within(container).getAllByRole('switch', {name: 'Formula'})[0];

const turnFormulaOnAndConfirm = async (container: HTMLElement) => {
    fireEvent.click(getFormulaSwitch(container));

    await settle();

    fireEvent.click(screen.getByRole('button', {name: 'Replace'}));

    await settle();
};

const setCurrentNodeParameters = (parameters: Record<string, unknown>) =>
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {
            displayConditions: {},
            metadata: {},
            name: 'trigger_1',
            parameters,
            workflowNodeName: 'trigger_1',
        },
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

describe('Formula switch on an object or array builder', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();
        (saveProperty as unknown as Mock).mockImplementation(respondLikeServer);
        (deleteProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-builder-formula', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);
    });

    it('turns only the object array item into an empty formula and keeps it a formula when the panel opens again', async () => {
        setCurrentNodeParameters({inputs: formInputs});

        const view = renderProperty(inputsProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        await turnFormulaOnAndConfirm(screen.getByLabelText('Array property item at index 3'));

        expect(saveRequests()).toEqual([{includeInMetadata: true, path: 'inputs[3]', type: 'OBJECT', value: '='}]);
        expect(deleteProperty).not.toHaveBeenCalled();
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.parameters).toEqual({
            inputs: [...formInputs.slice(0, 3), '='],
        });

        view.unmount();

        (saveProperty as unknown as Mock).mockClear();

        renderProperty(inputsProperty);

        await settle();

        expect(saveRequests()).toEqual([]);
        expect(getFormulaSwitch(screen.getByLabelText('Array property item at index 3'))).toBeChecked();
        expect(getFormulaSwitch(screen.getByLabelText('Array property item at index 2'))).not.toBeChecked();
    });

    it('turns an object property that is not an array item into an empty formula', async () => {
        setCurrentNodeParameters({settings: {tags: ['first'], text: 'hello'}});

        renderProperty(settingsProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        await turnFormulaOnAndConfirm(screen.getByLabelText('settings property'));

        expect(saveRequests()).toEqual([{includeInMetadata: true, path: 'settings', type: 'OBJECT', value: '='}]);
    });

    it('turns a nested array into an empty formula without touching its sibling', async () => {
        setCurrentNodeParameters({settings: {tags: ['first', 'second'], text: 'hello'}});

        renderProperty(settingsProperty);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        await turnFormulaOnAndConfirm(screen.getByLabelText('tags property'));

        expect(saveRequests()).toEqual([{includeInMetadata: true, path: 'settings.tags', type: 'ARRAY', value: '='}]);
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.parameters).toEqual({
            settings: {tags: '=', text: 'hello'},
        });
    });
});
