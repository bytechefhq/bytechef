import IntegrationInstanceConfigurationList from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpIntegrationInstanceConfigurationsQuery: () => ({data: undefined}),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationListItem',
    () => ({
        default: ({integrationInstanceConfiguration}: {integrationInstanceConfiguration: {name: string}}) => (
            <div>{`${integrationInstanceConfiguration.name} header`}</div>
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
});
