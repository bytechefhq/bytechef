vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen} from '@testing-library/react';
import {ReactNode} from 'react';
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

const settingsProperty = {
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
};

const renderArray = (wrap: (children: ReactNode) => ReactNode = (children) => children) =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                {wrap(<Property path={arrayProperty.name} property={arrayProperty} />)}
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

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

describe('picking an empty container opens the data pill panel', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-container-pill-panel', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useDataPillPanelStore.setState({dataPillPanelHasContent: true, dataPillPanelOpen: false});
    });

    it('opens the panel when the label of an empty array is clicked', async () => {
        setParameters({counts: []});

        renderArray();

        await settle();

        fireEvent.mouseDown(screen.getByText('Counts'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('leaves the panel closed when the label of a non-empty array is clicked', async () => {
        setParameters({counts: [5]});

        renderArray();

        await settle();

        fireEvent.mouseDown(screen.getByText('Counts'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('leaves the panel closed when the add item button is pressed', async () => {
        setParameters({counts: []});

        renderArray();

        await settle();

        fireEvent.mouseDown(screen.getByRole('button', {name: /add array item/i}));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });
});

describe('focusing a constant field opens the data pill panel', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-constant-pill-panel', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useDataPillPanelStore.setState({dataPillPanelHasContent: true, dataPillPanelOpen: false});
    });

    it('opens the panel when an integer input gets focus', async () => {
        setParameters({});

        renderProperty(countProperty);

        await settle();

        fireEvent.focus(screen.getByRole('spinbutton'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('opens the panel when a select gets focus', async () => {
        setParameters({method: 'GET'});

        renderProperty(methodProperty);

        await settle();

        fireEvent.focus(screen.getByRole('combobox'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('opens the panel when a field inside an object gets focus', async () => {
        setParameters({settings: {count: 5}});

        renderProperty(settingsProperty);

        await settle();

        fireEvent.focus(screen.getByRole('spinbutton'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('leaves the panel closed when the node details panel is closed', async () => {
        setParameters({});

        useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: false});

        renderProperty(countProperty);

        await settle();

        fireEvent.focus(screen.getByRole('spinbutton'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('leaves the panel closed when the field takes no expressions', async () => {
        setParameters({});

        renderProperty({...countProperty, expressionEnabled: false} as PropertyAllType);

        await settle();

        fireEvent.focus(screen.getByRole('spinbutton'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('leaves the panel closed when the add item button of an empty array gets focus', async () => {
        setParameters({counts: []});

        renderArray();

        await settle();

        fireEvent.focus(screen.getByRole('button', {name: /add array item/i}));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });
});
