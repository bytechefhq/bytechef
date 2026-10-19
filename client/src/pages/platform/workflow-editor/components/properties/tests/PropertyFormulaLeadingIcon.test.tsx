vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

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
};

const ControlledWrapper = ({value}: {value: unknown}) => {
    const form = useForm({defaultValues: {count: value}});

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property
                    control={form.control as never}
                    controlPath=""
                    formState={form.formState}
                    property={countProperty}
                    toolsMode
                />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

describe('Formula mode leading icon', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-formula-icon', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {count: '=5'}, workflowNodeName: 'node_1'},
            pillTarget: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('an uncontrolled formula field shows its type icon, not the formula icon', async () => {
        const {container} = render(
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property parameterValue="=5" path="count" property={countProperty} />
                </WorkflowEditorProvider>
            </TooltipProvider>
        );

        await settle();

        expect(container.querySelector('.ProseMirror')).not.toBeNull();
        expect(container.querySelector('.lucide-hash')).not.toBeNull();
        expect(container.querySelector('.lucide-square-function')).toBeNull();
    });

    it('a controlled formula field shows its type icon, not the formula icon', async () => {
        const {container} = render(<ControlledWrapper value="=5" />);

        await settle();

        expect(container.querySelector('.ProseMirror')).not.toBeNull();
        expect(container.querySelector('.lucide-hash')).not.toBeNull();
        expect(container.querySelector('.lucide-square-function')).toBeNull();
    });
});
