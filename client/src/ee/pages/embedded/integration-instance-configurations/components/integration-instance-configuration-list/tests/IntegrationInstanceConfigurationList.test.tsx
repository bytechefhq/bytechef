import IntegrationInstanceConfigurationList from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

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

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItem',
    () => ({
        default: ({
            integrationInstanceConfiguration,
            mcpWorkflowIds,
        }: {
            integrationInstanceConfiguration: {name: string};
            mcpWorkflowIds?: Set<string>;
        }) => (
            <div>
                <div>{`${integrationInstanceConfiguration.name} header`}</div>

                <div>{`${integrationInstanceConfiguration.name} MCP workflows: ${Array.from(mcpWorkflowIds || []).join(',') || 'none'}`}</div>
            </div>
        ),
    })
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList',
    () => ({
        default: ({integrationInstanceConfigurationId}: {integrationInstanceConfigurationId: number}) => (
            <div>{`Workflows of ${integrationInstanceConfigurationId}`}</div>
        ),
    })
);

const integration = {componentName: 'asana', id: 1, multipleInstances: false, name: 'Asana'} as Integration;

const integrationInstanceConfigurations = [
    {environmentId: 0, id: 11, integrationId: 1, integrationVersion: 1, name: 'First'},
    {environmentId: 0, id: 12, integrationId: 1, integrationVersion: 1, name: 'Second'},
] as IntegrationInstanceConfiguration[];

const mcpIntegrationInstanceConfigurationsData = {
    mcpIntegrationInstanceConfigurations: [
        {
            integrationInstanceConfigurationId: '11',
            mcpIntegrationInstanceConfigurationWorkflows: [
                {integrationInstanceConfigurationWorkflow: {workflowId: 'workflow-1'}},
            ],
        },
    ],
};

const renderList = (expandAll?: boolean) =>
    render(
        <IntegrationInstanceConfigurationList
            expandAll={expandAll}
            integration={integration}
            integrationInstanceConfigurations={integrationInstanceConfigurations}
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

    it('keeps every instance configuration collapsed by default', () => {
        renderList();

        expect(screen.getByText('First header')).toBeInTheDocument();
        expect(screen.queryByText(/Workflows of/)).not.toBeInTheDocument();
    });

    it('expands every instance configuration when asked to expand all', () => {
        renderList(true);

        expect(screen.getByText('Workflows of 11')).toBeInTheDocument();
        expect(screen.getByText('Workflows of 12')).toBeInTheDocument();
    });

    it('marks the MCP workflows of a configuration for a tenant admin', () => {
        renderList();

        expect(screen.getByText('First MCP workflows: workflow-1')).toBeInTheDocument();
    });

    it('does not read MCP configurations or mark MCP workflows for a user who is not a tenant admin', () => {
        hoisted.isTenantAdmin = false;

        renderList();

        expect(hoisted.useMcpIntegrationInstanceConfigurationsQuery).toHaveBeenCalledWith(undefined, {enabled: false});
        expect(screen.getByText('First MCP workflows: none')).toBeInTheDocument();
    });
});
