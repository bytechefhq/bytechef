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
import {StrictMode} from 'react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const enabledProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Enabled',
    name: 'enabled',
    type: 'BOOLEAN',
} as PropertyAllType;

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

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

const renderProperty = (property: PropertyAllType, value: unknown) => {
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {name: 'node_1', parameters: {[property.name!]: value}, workflowNodeName: 'node_1'},
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

    return render(
        <StrictMode>
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property parameterValue={value} path={property.name} property={property} />
                </WorkflowEditorProvider>
            </TooltipProvider>
        </StrictMode>
    );
};

const savedValues = () => (saveProperty as unknown as Mock).mock.calls.map(([request]) => request.value);

describe('Formula switch from a native control', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-fresh-editor', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({pillTarget: null, workflowNodeDetailsPanelOpen: true});
    });

    it('shows a BOOLEAN select value in the formula editor and saves only the converted values', async () => {
        const {container} = renderProperty(enabledProperty, true);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(container.querySelector('.ProseMirror')?.textContent).toBe('true');
        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'true');
        expect(savedValues()).toEqual(['=true']);

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(savedValues()).toEqual(['=true', true]);
    });

    it('shows an INTEGER value in the formula editor and saves only the converted values', async () => {
        const {container} = renderProperty(countProperty, 5);

        await settle();

        (saveProperty as unknown as Mock).mockClear();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(container.querySelector('.ProseMirror')?.textContent).toBe('5');
        expect(savedValues()).toEqual(['=5']);

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(savedValues()).toEqual(['=5', 5]);
    });
});
