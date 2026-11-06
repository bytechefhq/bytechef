import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurations from '@/ee/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    integrationInstanceConfigurations: [] as IntegrationInstanceConfiguration[],
    integrations: [] as Integration[],
    integrationsFilters: [] as unknown[],
    openIntegrationMock: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: (filters: unknown) => {
        hoisted.integrationsFilters.push(filters);

        return {data: hoisted.integrations, error: null, isLoading: false};
    },
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationsQuery: () => ({
        data: hoisted.integrationInstanceConfigurations,
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries', () => ({
    useGetIntegrationInstanceConfigurationTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/pages/embedded/shared/useOpenInEmbeddedEditor', () => ({
    default: () => ({openIntegration: hoisted.openIntegrationMock}),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-list/IntegrationInstanceConfigurationList',
    () => ({
        default: ({
            integration,
            showIntegrationIcon,
            showOpenInIntegration,
        }: {
            integration: Integration;
            showIntegrationIcon?: boolean;
            showOpenInIntegration?: boolean;
        }) => (
            <div>{`${integration.name} configurations icon=${showIntegrationIcon} open=${showOpenInIntegration}`}</div>
        ),
    })
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({default: () => null})
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-executions-sheet/IntegrationInstanceConfigurationWorkflowExecutionsSheet',
    () => ({default: () => null})
);

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <svg data-src={src} data-testid="integration-icon" />,
}));

const gmail = {componentName: 'gmail', icon: 'gmail.svg', id: 1, integrationWorkflowIds: [21], name: 'Gmail'};

const affinity = {componentName: 'affinity', id: 2, integrationWorkflowIds: [], name: 'Affinity'};

const renderPage = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationInstanceConfigurations />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('IntegrationInstanceConfigurations', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.integrations = [gmail, affinity] as Integration[];
        hoisted.integrationInstanceConfigurations = [
            {id: 11, integration: gmail, integrationId: 1, name: 'Gmail production'},
            {id: 12, integration: affinity, integrationId: 2, name: 'Affinity production'},
        ] as unknown as IntegrationInstanceConfiguration[];
        hoisted.integrationsFilters = [];
        hoisted.openIntegrationMock.mockReset();
    });

    afterEach(() => {
        resetAll();
    });

    it('loads the integrations with all fields so the headers get their icon and workflows', () => {
        renderPage();

        expect(hoisted.integrationsFilters).toContainEqual({
            includeAllFields: true,
            integrationInstanceConfigurations: true,
        });
    });

    it('groups the configurations under a header per integration', () => {
        renderPage();

        expect(screen.getByRole('heading', {name: 'Gmail'})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Affinity'})).toBeInTheDocument();
        expect(screen.getByTestId('integration-icon')).toHaveAttribute('data-src', 'gmail.svg');
        expect(screen.getByText('Gmail configurations icon=false open=true')).toBeInTheDocument();
    });

    it('offers opening only the integrations that have workflows', async () => {
        const user = userEvent.setup();

        renderPage();

        const openIntegrationButtons = screen.getAllByRole('button', {name: 'Open integration'});

        expect(openIntegrationButtons).toHaveLength(1);

        await user.click(openIntegrationButtons[0]);

        expect(hoisted.openIntegrationMock).toHaveBeenCalledWith(gmail);
    });
});
