vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const schemaProperty = {
    controlType: 'JSON_SCHEMA_BUILDER',
    label: 'Response',
    name: 'schema',
    type: 'STRING',
} as PropertyAllType;

const renderSchemaProperty = (property: PropertyAllType) =>
    render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property path={property.name} property={property} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

describe('JSON schema builder Formula switch', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-schema-formula', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {}, workflowNodeName: 'node_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('shows the switch when expressions are enabled', () => {
        renderSchemaProperty({...schemaProperty, expressionEnabled: true} as PropertyAllType);

        expect(screen.getByRole('switch', {name: 'Formula'})).toBeInTheDocument();
    });

    it('hides the switch when expressions are disabled', () => {
        renderSchemaProperty({...schemaProperty, expressionEnabled: false} as PropertyAllType);

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
    });

    it('renders the builder for a stored value that is not a JSON schema', () => {
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'node_1', parameters: {schema: 'hello'}, workflowNodeName: 'node_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

        renderSchemaProperty({...schemaProperty, expressionEnabled: true} as PropertyAllType);

        expect(screen.getByText('Response')).toBeInTheDocument();
    });
});
