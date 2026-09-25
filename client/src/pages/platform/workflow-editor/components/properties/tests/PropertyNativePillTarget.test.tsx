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
import {act, fireEvent, waitFor} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const booleanProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Enabled',
    name: 'enabled',
    type: 'BOOLEAN',
} as PropertyAllType;

const renderUncontrolled = (property: PropertyAllType, parameterValue?: unknown) =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property parameterValue={parameterValue} path={`parameters.${property.name}`} property={property} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

const ControlledWrapper = ({property}: {property: PropertyAllType}) => {
    const form = useForm({defaultValues: {count: 5}});

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property
                    control={form.control as never}
                    controlPath=""
                    formState={form.formState}
                    property={property}
                    toolsMode
                />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

describe('native pill target', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-native-pill', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {}, workflowNodeName: 'node_1'},
            pillTarget: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('focusing a number input registers it, and a pill replaces its constant', async () => {
        const {container} = renderUncontrolled(countProperty, 5);

        fireEvent.focus(container.querySelector('input')!);

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        expect(pillTarget).not.toBeNull();

        act(() => pillTarget!.insertPill('trigger_1.count'));

        await waitFor(() => expect(container.querySelector('.ProseMirror')).not.toBeNull());

        expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: '${trigger_1.count}'}));
    });

    it('focusing a boolean select registers it instead of the previously focused editor', () => {
        const {container} = renderUncontrolled(booleanProperty, true);

        useWorkflowNodeDetailsPanelStore.setState({
            pillTarget: {acceptsPill: () => true, insertPill: vi.fn(), owner: 'stale-editor'},
        });

        fireEvent.focus(container.querySelector('button')!);

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget?.owner).not.toBe('stale-editor');
    });

    it('dropping a pill onto a native field replaces its value', async () => {
        const {container} = renderUncontrolled(countProperty, 5);

        const dataTransfer = {
            getData: () => JSON.stringify({mentionId: 'trigger_1.count'}),
            types: ['application/bytechef-datapill'],
        };

        fireEvent.drop(container.querySelector('input')!, {dataTransfer});

        await waitFor(() =>
            expect(saveProperty).toHaveBeenLastCalledWith(expect.objectContaining({value: '${trigger_1.count}'}))
        );
    });

    it('a fromAi field does not accept pills', () => {
        const {container} = renderUncontrolled(countProperty, "=fromAi('count', 'INTEGER', {'required': false})");

        container
            .querySelector('[aria-label="count property"]')
            ?.dispatchEvent(new FocusEvent('focusin', {bubbles: true}));

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        expect(pillTarget === null || pillTarget.acceptsPill() === false).toBe(true);
    });

    it('a controlled field does not accept pills', () => {
        const {container} = render(<ControlledWrapper property={countProperty} />);

        fireEvent.focus(container.querySelector('input')!);

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        expect(pillTarget === null || pillTarget.acceptsPill() === false).toBe(true);
    });
});
