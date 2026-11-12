import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationConfigurationsSheet from '@/ee/pages/embedded/integration/components/IntegrationConfigurationsSheet';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    configurationsQuery: vi.fn(),
    navigate: vi.fn(),
    panelProps: vi.fn(),
    refetch: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationsQuery: (filters: unknown, enabled: boolean) => {
        hoisted.configurationsQuery(filters, enabled);

        return {isFetching: false, refetch: hoisted.refetch};
    },
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsPanel',
    () => ({
        default: (props: {enabled: boolean; integration: Integration}) => {
            hoisted.panelProps(props);

            return <div>{`Configurations of ${props.integration.name}`}</div>;
        },
    })
);

const onSheetOpenChange = vi.fn();

const integration = {id: 5, name: 'Gmail'} as Integration;

const renderIntegrationConfigurationsSheet = () =>
    render(
        <TooltipProvider>
            <IntegrationConfigurationsSheet integration={integration} onSheetOpenChange={onSheetOpenChange} sheetOpen />
        </TooltipProvider>
    );

describe('IntegrationConfigurationsSheet', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows the development configurations of the integration', () => {
        renderIntegrationConfigurationsSheet();

        expect(screen.getByText('Integration Configurations')).toBeInTheDocument();
        expect(screen.getByText('Configurations of Gmail')).toBeInTheDocument();
        expect(hoisted.panelProps).toHaveBeenCalledWith({enabled: true, integration});
        expect(hoisted.configurationsQuery).toHaveBeenCalledWith({environmentId: 0, integrationId: 5}, true);
    });

    it('refetches the configurations from the refresh button', async () => {
        renderIntegrationConfigurationsSheet();

        await userEvent.click(screen.getByRole('button', {name: 'Refresh integration configurations'}));

        expect(hoisted.refetch).toHaveBeenCalledTimes(1);
    });

    it('closes from the Close button', async () => {
        renderIntegrationConfigurationsSheet();

        const footerCloseButton = screen
            .getAllByRole('button', {name: 'Close'})
            .find((button) => button.textContent === 'Close');

        await userEvent.click(footerCloseButton!);

        expect(onSheetOpenChange).toHaveBeenCalledWith(false);
        expect(hoisted.navigate).not.toHaveBeenCalled();
    });

    it('offers only Close in the footer, without a link to the configurations page', () => {
        renderIntegrationConfigurationsSheet();

        expect(screen.queryByRole('button', {name: 'Open in Configurations'})).not.toBeInTheDocument();
    });
});
