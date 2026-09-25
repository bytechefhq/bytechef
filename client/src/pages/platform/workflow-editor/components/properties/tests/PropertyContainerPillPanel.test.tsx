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

    it('opens the panel when the hint of an empty array is clicked', async () => {
        setParameters({counts: []});

        renderArray();

        await settle();

        fireEvent.mouseDown(screen.getByText('Drop or click a data pill'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
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

    it('keeps the hint clear of the add item button', async () => {
        setParameters({counts: []});

        renderArray();

        await settle();

        expect(screen.getByText('Drop or click a data pill')).toHaveClass('mb-2');
    });
});
