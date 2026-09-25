vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

vi.mock(
    '@/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilder',
    () => ({
        default: ({onChange}: {onChange: (value?: Record<string, unknown>) => void}) => (
            <div>
                <button type="button">Open Response Template</button>

                <button onClick={() => onChange({properties: {name: {type: 'string'}}, type: 'object'})} type="button">
                    Draft schema
                </button>

                <button onClick={() => onChange({})} type="button">
                    Clear schema
                </button>
            </div>
        ),
    })
);

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
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const schemaProperty = {
    controlType: 'JSON_SCHEMA_BUILDER',
    expressionEnabled: true,
    label: 'Response',
    name: 'schema',
    type: 'STRING',
} as PropertyAllType;

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await act(async () => {
            await new Promise<void>((resolve) => setTimeout(resolve, 0));
        });
    }
};

const renderSchemaBuilder = () =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property path="schema" property={schemaProperty} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

const pillSaves = () =>
    (saveProperty as unknown as Mock).mock.calls.filter(
        ([request]) => typeof request.value === 'string' && request.value.startsWith('${')
    );

describe('JSON schema builder with a schema that is not saved yet', () => {
    beforeEach(() => {
        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-schema-draft', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {schema: ''}, workflowNodeName: 'node_1'},
            pillTarget: null,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('does not take a pill while a drafted schema waits to save', async () => {
        renderSchemaBuilder();

        await settle();

        fireEvent.click(screen.getByRole('button', {name: 'Draft schema'}));

        act(() => screen.getByRole('button', {name: 'Open Response Template'}).focus());

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        expect(pillTarget).not.toBeNull();
        expect(pillTarget!.acceptsPill()).toBe(false);

        act(() => pillTarget!.insertPill('trigger_1.schema'));

        fireEvent.drop(screen.getByRole('button', {name: 'Open Response Template'}), {
            dataTransfer: {
                getData: () => JSON.stringify({mentionId: 'trigger_1.schema'}),
                types: ['application/bytechef-datapill'],
            },
        });

        await settle();

        expect(pillSaves()).toEqual([]);
    });

    it('takes a pill again once the drafted schema is cleared and nothing was saved', async () => {
        renderSchemaBuilder();

        await settle();

        fireEvent.click(screen.getByRole('button', {name: 'Draft schema'}));
        fireEvent.click(screen.getByRole('button', {name: 'Clear schema'}));

        act(() => screen.getByRole('button', {name: 'Open Response Template'}).focus());

        const pillTarget = useWorkflowNodeDetailsPanelStore.getState().pillTarget;

        expect(pillTarget!.acceptsPill()).toBe(true);

        act(() => pillTarget!.insertPill('trigger_1.schema'));

        await settle();

        expect(saveProperty).toHaveBeenCalledWith(
            expect.objectContaining({path: 'schema', value: '${trigger_1.schema}'})
        );
    });
});
