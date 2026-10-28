import IntegrationInstanceConfigurationsPanel from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsPanel';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    error: null as Error | null,
    integrationInstanceConfigurations: [] as IntegrationInstanceConfiguration[] | undefined,
    isLoading: false,
    queryRequests: [] as Array<{enabled?: boolean; filters: unknown}>,
    refetch: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationsQuery: (filters: unknown, enabled?: boolean) => {
        hoisted.queryRequests.push({enabled, filters});

        return {
            data: hoisted.integrationInstanceConfigurations,
            error: hoisted.error,
            isLoading: hoisted.isLoading,
            refetch: hoisted.refetch,
        };
    },
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries', () => ({
    useGetIntegrationInstanceConfigurationTagsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList',
    () => ({
        default: ({
            expandAll,
            integrationInstanceConfigurations,
        }: {
            expandAll?: boolean;
            integrationInstanceConfigurations: IntegrationInstanceConfiguration[];
        }) => (
            <ul data-expand-all={String(!!expandAll)} data-testid="integration-instance-configuration-list">
                {integrationInstanceConfigurations.map((integrationInstanceConfiguration) => (
                    <li key={integrationInstanceConfiguration.id}>{integrationInstanceConfiguration.name}</li>
                ))}
            </ul>
        ),
    })
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: ({
            integrationInstanceConfiguration,
            triggerNode,
        }: {
            integrationInstanceConfiguration: IntegrationInstanceConfiguration;
            triggerNode: React.ReactNode;
        }) => (
            <>
                {triggerNode}

                <span>{`Configuration dialog for integration ${integrationInstanceConfiguration.integrationId} in environment ${integrationInstanceConfiguration.environmentId}`}</span>
            </>
        ),
    })
);

const integration = {
    componentName: 'asana',
    id: 7,
    lastIntegrationVersion: 2,
    lastPublishedDate: new Date(),
    multipleInstances: false,
    name: 'Asana',
} as Integration;

const renderPanel = (integrationOverrides: Partial<Integration> = {}) =>
    render(<IntegrationInstanceConfigurationsPanel enabled integration={{...integration, ...integrationOverrides}} />);

describe('IntegrationInstanceConfigurationsPanel', () => {
    beforeEach(() => {
        hoisted.error = null;
        hoisted.integrationInstanceConfigurations = [];
        hoisted.isLoading = false;
        hoisted.queryRequests = [];
        hoisted.refetch.mockReset();
    });

    it("loads only the integration's Development instance configurations", () => {
        renderPanel();

        expect(hoisted.queryRequests[0]).toEqual({enabled: true, filters: {environmentId: 0, integrationId: 7}});
    });

    it('lists the instance configurations sorted by name and expanded', () => {
        hoisted.integrationInstanceConfigurations = [
            {environmentId: 0, id: 2, integrationId: 7, integrationVersion: 1, name: 'Zeta'},
            {environmentId: 0, id: 1, integrationId: 7, integrationVersion: 1, name: 'Alpha'},
        ] as IntegrationInstanceConfiguration[];

        renderPanel();

        const list = screen.getByTestId('integration-instance-configuration-list');

        expect(list).toHaveAttribute('data-expand-all', 'true');
        expect(list).toHaveTextContent('AlphaZeta');
    });

    it('offers creating a Development configuration when there is none and the integration is published', () => {
        renderPanel();

        expect(screen.getByText('No configurations in Development')).toBeInTheDocument();
        expect(screen.getByText('Configuration dialog for integration 7 in environment 0')).toBeInTheDocument();
    });

    it('asks to publish the integration first when it has no published version', () => {
        renderPanel({lastPublishedDate: undefined});

        expect(screen.getByText('Publish the integration to configure it')).toBeInTheDocument();
        expect(screen.queryByText(/Configuration dialog/)).not.toBeInTheDocument();
    });

    it('offers a retry when the instance configurations fail to load', () => {
        hoisted.error = new Error('failed');
        hoisted.integrationInstanceConfigurations = undefined;

        renderPanel();

        fireEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(hoisted.refetch).toHaveBeenCalled();
    });
});
