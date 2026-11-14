import {Popover} from '@/components/ui/popover';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationWorkflowPropertiesPopover from '../McpAutomationProjectConfigurationWorkflowPropertiesPopover';
import {McpAutomationProjectConfigurationWorkflowItemType} from '../hooks/useMcpAutomationProjectConfigurationList';

const hoisted = vi.hoisted(() => ({
    propertiesResult: {
        data: {
            mcpAutomationProjectConfigurationWorkflowProperties: [
                {name: 'toolName', type: 'STRING'},
                {name: 'customerId', type: 'STRING'},
            ],
        },
        isLoading: false,
    },
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useMcpAutomationProjectConfigurationWorkflowPropertiesQuery: () => hoisted.propertiesResult,
    useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({properties}: {properties: {name: string}[]}) => (
        <ul>
            {properties.map((property) => (
                <li key={property.name}>{property.name}</li>
            ))}
        </ul>
    ),
}));

const workflow = {
    id: 'w1',
    label: 'Workflow One',
    parameters: null,
} as McpAutomationProjectConfigurationWorkflowItemType;

describe('McpAutomationProjectConfigurationWorkflowPropertiesPopover', () => {
    it('listsThePropertiesFromTheQuery', () => {
        render(
            <Popover open>
                <McpAutomationProjectConfigurationWorkflowPropertiesPopover onClose={vi.fn()} workflow={workflow} />
            </Popover>
        );

        expect(screen.getByText('Edit Workflow One Tool')).toBeInTheDocument();
        expect(screen.getByText('toolName')).toBeInTheDocument();
        expect(screen.getByText('customerId')).toBeInTheDocument();
    });
});
