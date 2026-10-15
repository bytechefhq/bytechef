import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationList from '../IntegrationInstanceConfigurationList';

const hoisted = vi.hoisted(() => ({
    isTenantAdmin: true,
    useMcpIntegrationInstanceConfigurationsQuery: vi.fn(),
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpIntegrationInstanceConfigurationsQuery: hoisted.useMcpIntegrationInstanceConfigurationsQuery,
}));

vi.mock('../IntegrationInstanceConfigurationListItem', () => ({
    default: ({mcpWorkflowIds}: {mcpWorkflowIds?: Set<string>}) => (
        <div>{`MCP workflows: ${Array.from(mcpWorkflowIds || []).join(',') || 'none'}`}</div>
    ),
}));

vi.mock('../../integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList', () => ({
    default: () => null,
}));

const mcpIntegrationInstanceConfigurationsData = {
    mcpIntegrationInstanceConfigurations: [
        {
            integrationInstanceConfigurationId: '7',
            mcpIntegrationInstanceConfigurationWorkflows: [
                {integrationInstanceConfigurationWorkflow: {workflowId: 'workflow-1'}},
            ],
        },
    ],
};

const renderList = () =>
    render(
        <IntegrationInstanceConfigurationList
            integration={{componentName: 'hubspot', id: 3} as Integration}
            integrationInstanceConfigurations={[{id: 7} as IntegrationInstanceConfiguration]}
            tags={[]}
        />
    );

describe('IntegrationInstanceConfigurationList', () => {
    beforeEach(() => {
        hoisted.isTenantAdmin = true;

        hoisted.useMcpIntegrationInstanceConfigurationsQuery.mockReset();
        hoisted.useMcpIntegrationInstanceConfigurationsQuery.mockImplementation(
            (_variables: unknown, options?: {enabled?: boolean}) => ({
                data: options?.enabled === false ? undefined : mcpIntegrationInstanceConfigurationsData,
            })
        );
    });

    it('marks the MCP workflows of a configuration for a tenant admin', () => {
        renderList();

        expect(screen.getByText('MCP workflows: workflow-1')).toBeInTheDocument();
    });

    it('does not read MCP configurations or mark MCP workflows for a user who is not a tenant admin', () => {
        hoisted.isTenantAdmin = false;

        renderList();

        expect(hoisted.useMcpIntegrationInstanceConfigurationsQuery).toHaveBeenCalledWith(undefined, {enabled: false});
        expect(screen.getByText('MCP workflows: none')).toBeInTheDocument();
    });
});
