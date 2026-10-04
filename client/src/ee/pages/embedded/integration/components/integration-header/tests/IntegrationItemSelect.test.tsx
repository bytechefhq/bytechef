import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationItemSelect from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationItemSelect';
import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockOnWorkflowValueChange = vi.fn();

const mockIntegrationWorkflows = [
    {integrationWorkflowId: 1111, label: 'Workflow 1'},
    {integrationWorkflowId: 2222, label: 'Workflow 2'},
];

const renderIntegrationItemSelect = (props: Partial<React.ComponentProps<typeof IntegrationItemSelect>> = {}) =>
    render(
        <TooltipProvider>
            <IntegrationItemSelect
                currentIntegrationWorkflowId={1111}
                currentLabel="Workflow 1"
                integrationWorkflows={mockIntegrationWorkflows}
                onWorkflowValueChange={mockOnWorkflowValueChange}
                {...props}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

it('shows the closed select with the current workflow label', () => {
    renderIntegrationItemSelect();

    expect(screen.getByLabelText('Integration item select')).toBeInTheDocument();
    expect(screen.getByText('Workflow 1')).toBeInTheDocument();
    expect(screen.queryByText('Workflow 2')).not.toBeInTheDocument();
});

it('lists the integration workflows under a Workflows group once opened', async () => {
    renderIntegrationItemSelect();

    await userEvent.click(screen.getByLabelText('Integration item select'));

    expect(screen.getByText('Workflows')).toBeInTheDocument();
    expect(screen.getByText('Workflow 2')).toBeInTheDocument();
});

it('omits the Workflows group header when the integration has no workflows', async () => {
    renderIntegrationItemSelect({integrationWorkflows: []});

    await userEvent.click(screen.getByLabelText('Integration item select'));

    expect(screen.queryByText('Workflows')).not.toBeInTheDocument();
});

it('marks the current workflow as the checked menu item', async () => {
    renderIntegrationItemSelect();

    await userEvent.click(screen.getByLabelText('Integration item select'));

    const menu = screen.getByRole('menu');

    expect(within(menu).getByText('Workflow 1').closest('[role="menuitemradio"]')).toHaveAttribute(
        'aria-checked',
        'true'
    );
    expect(within(menu).getByText('Workflow 2').closest('[role="menuitemradio"]')).toHaveAttribute(
        'aria-checked',
        'false'
    );
});

it('calls onWorkflowValueChange with the selected integration workflow id', async () => {
    renderIntegrationItemSelect();

    await userEvent.click(screen.getByLabelText('Integration item select'));
    await userEvent.click(screen.getByText('Workflow 2'));

    expect(mockOnWorkflowValueChange).toHaveBeenCalledWith(2222);
});

it('shows a skeleton while the current workflow label is loading', () => {
    renderIntegrationItemSelect({currentLabel: undefined});

    expect(screen.queryByText('Workflow 1')).not.toBeInTheDocument();
});

it('shows the full label of a long current workflow in a tooltip', async () => {
    const longLabel = 'A workflow label that is long enough to be truncated';

    renderIntegrationItemSelect({currentLabel: longLabel});

    await userEvent.hover(screen.getByLabelText('Integration item select'));

    expect(await screen.findByRole('tooltip')).toHaveTextContent(longLabel);
});

it('titles long workflow labels in the menu with their full text', async () => {
    const longLabel = 'A workflow label that is long enough to overflow the content-sized menu width';

    renderIntegrationItemSelect({
        integrationWorkflows: [...mockIntegrationWorkflows, {integrationWorkflowId: 3333, label: longLabel}],
    });

    await userEvent.click(screen.getByLabelText('Integration item select'));

    const menu = screen.getByRole('menu');

    expect(within(menu).getByText(longLabel).closest('[role="menuitemradio"]')).toHaveAttribute('title', longLabel);
    expect(within(menu).getByText('Workflow 2').closest('[role="menuitemradio"]')).not.toHaveAttribute('title');
});

it('checks no workflow without a current integration workflow', async () => {
    renderIntegrationItemSelect({currentIntegrationWorkflowId: undefined});

    await userEvent.click(screen.getByLabelText('Integration item select'));

    const menu = screen.getByRole('menu');

    expect(within(menu).getByText('Workflow 1').closest('[role="menuitemradio"]')).toHaveAttribute(
        'aria-checked',
        'false'
    );
});
