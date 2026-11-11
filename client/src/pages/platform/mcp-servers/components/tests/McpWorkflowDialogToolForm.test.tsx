import useMcpWorkflowToolForms, {
    McpWorkflowToolFormsType,
    SavedToolParametersByWorkflowIdType,
} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolForms';
import {PropertyAllType} from '@/shared/types';
import {act, render, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpWorkflowDialogToolForm from '../McpWorkflowDialogToolForm';

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({properties}: {properties: Array<{name: string}>}) => (
        <div data-testid="properties">{properties.map((property) => property.name).join(',')}</div>
    ),
}));

const properties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', name: 'message', type: 'STRING'},
] as Array<PropertyAllType>;

const loadWorkflowProperties = vi.fn(async () => properties);

const savedParametersByWorkflowId: SavedToolParametersByWorkflowIdType = new Map([
    ['workflow-1', {message: null, toolName: 'saved_tool'}],
]);

const selectedWorkflowIds = ['workflow-1'];

let toolFormsRef: McpWorkflowToolFormsType | undefined;

const Harness = () => {
    const toolForms = useMcpWorkflowToolForms({
        loadWorkflowProperties,
        savedParametersByWorkflowId,
        seedKey: 'seed',
        selectedWorkflowIds,
    });

    toolFormsRef = toolForms;

    return (
        <McpWorkflowDialogToolForm
            isLoading={false}
            properties={properties}
            toolForms={toolForms}
            workflowId="workflow-1"
        />
    );
};

describe('McpWorkflowDialogToolForm', () => {
    beforeEach(() => {
        toolFormsRef = undefined;
    });

    it('shows a saved null as an empty string and does not report it as changed', async () => {
        render(<Harness />);

        const toolForm = toolFormsRef!.getToolForm('workflow-1');

        await waitFor(() => expect(toolForm.formControl.getValues('message')).toBe(''));

        act(() => toolForm.formControl.setValue('message', '', {shouldDirty: true}));

        let toolParameters: Awaited<ReturnType<McpWorkflowToolFormsType['buildToolParameters']>> = [];

        await act(async () => {
            toolParameters = await toolFormsRef!.buildToolParameters(['workflow-1']);
        });

        expect(toolParameters).toEqual([]);
    });
});
