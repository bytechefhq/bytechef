import {McpServer} from '@/shared/middleware/graphql';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpIntegrationInstanceConfigurationWorkflowDialog from './McpIntegrationInstanceConfigurationWorkflowDialog';
import {McpIntegrationInstanceConfigurationItemType} from './mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants — vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    eligibleResult: {data: undefined} as {data: unknown},
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useCreateMcpIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.createMutate, reset: vi.fn()}),
        useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery: () => hoisted.eligibleResult,
        useUpdateMcpIntegrationInstanceConfigurationMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
    };
});

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationsQuery: () => ({data: []}),
}));

const mcpServer = {id: 's1', name: 'Server'} as McpServer;

const makeEditConfig = (workflowLabels: string[]): McpIntegrationInstanceConfigurationItemType =>
    ({
        id: 'cfg1',
        integration: {name: 'Integration'},
        integrationInstanceConfigurationId: '42',
        integrationVersion: 1,
        mcpIntegrationInstanceConfigurationWorkflows: workflowLabels.map((label, index) => ({
            id: `mw${index}`,
            workflow: {label},
        })),
        mcpServerId: 's1',
    }) as McpIntegrationInstanceConfigurationItemType;

describe('McpIntegrationInstanceConfigurationWorkflowDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.eligibleResult.data = undefined;

        Element.prototype.scrollIntoView = vi.fn();
    });

    it('shows an explanatory message and keeps the submit button disabled when there are no tool-eligible workflows', () => {
        hoisted.eligibleResult.data = {toolEligibleIntegrationInstanceConfigurationWorkflows: []};

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig([])}
                mcpServer={mcpServer}
            />
        );

        expect(screen.getByText(/No tool-eligible workflows found/i)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Update'})).toBeDisabled();
    });

    it('enables the submit button once a tool-eligible workflow is selected', async () => {
        hoisted.eligibleResult.data = {
            toolEligibleIntegrationInstanceConfigurationWorkflows: [{id: 'w1', label: 'Workflow One'}],
        };

        render(
            <McpIntegrationInstanceConfigurationWorkflowDialog
                mcpIntegrationInstanceConfiguration={makeEditConfig(['Workflow One'])}
                mcpServer={mcpServer}
            />
        );

        // Edit mode pre-populates the previously-selected workflow once eligible workflows load.
        await waitFor(() => expect(screen.getByRole('button', {name: 'Update'})).toBeEnabled());

        expect(screen.queryByText(/No tool-eligible workflows found/i)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Update'}));

        expect(hoisted.updateMutate).toHaveBeenCalledTimes(1);
        expect(hoisted.updateMutate.mock.calls[0][0]).toMatchObject({
            id: 'cfg1',
            input: {selectedWorkflowIds: ['w1']},
        });
    });
});
