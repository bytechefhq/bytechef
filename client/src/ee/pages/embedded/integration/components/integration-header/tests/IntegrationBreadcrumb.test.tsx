import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationBreadcrumb from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {expect, it, vi} from 'vitest';

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <svg data-src={src} data-testid="integration-icon" />,
}));

const mockIntegration = {
    componentName: 'gmail',
    id: 1,
    lastIntegrationVersion: 3,
    lastStatus: IntegrationStatus.Draft,
    multipleInstances: false,
    name: 'Gmail',
};

it('shows the integration title with its version and status', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationBreadcrumb integration={mockIntegration} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByRole('heading', {name: 'Gmail'})).toBeInTheDocument();
    expect(screen.getByText('V3')).toBeInTheDocument();
    expect(screen.getByText(IntegrationStatus.Draft)).toBeInTheDocument();
});

it('shows the item switcher after a separator when one is given', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationBreadcrumb integration={mockIntegration} itemSelect={<button>workflow2</button>} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByRole('button', {name: 'workflow2'})).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
});

it('omits the separator and switcher without an item switcher', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationBreadcrumb integration={mockIntegration} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(2);
});

it('links back to the Integrations list page first', () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationBreadcrumb integration={mockIntegration} />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByRole('link', {name: 'Integrations'})).toHaveAttribute('href', '/embedded/integrations');
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('Integrations');
});

it('switches to another integration from the title select', async () => {
    const onIntegrationValueChange = vi.fn();

    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationBreadcrumb
                    integration={mockIntegration}
                    integrations={[
                        {...mockIntegration, integrationWorkflowIds: [11]},
                        {...mockIntegration, icon: 'slack.svg', id: 2, integrationWorkflowIds: [21], name: 'Slack'},
                        {...mockIntegration, id: 3, integrationWorkflowIds: [], name: 'Jira'},
                    ]}
                    onIntegrationValueChange={onIntegrationValueChange}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

    await userEvent.click(screen.getByLabelText('Integration select'));

    expect(screen.getByRole('menuitemradio', {name: 'Gmail'})).toHaveAttribute('data-state', 'checked');
    expect(screen.getByRole('menuitemradio', {name: 'Jira'})).toHaveAttribute('data-disabled');

    expect(screen.getByRole('menuitemradio', {name: 'Slack'})).toContainElement(screen.getByTestId('integration-icon'));
    expect(screen.getByTestId('integration-icon')).toHaveAttribute('data-src', 'slack.svg');

    await userEvent.click(screen.getByRole('menuitemradio', {name: 'Slack'}));

    expect(onIntegrationValueChange).toHaveBeenCalledWith(2);
});
