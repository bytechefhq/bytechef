import {TooltipProvider} from '@/components/ui/tooltip';
import SettingsMenu from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationDialog', () => ({
    default: () => <div role="dialog">Edit Integration</div>,
}));

const mockIntegration = {
    componentName: 'gmail',
    id: 1,
    multipleInstances: false,
    name: 'Gmail',
};

const mockWorkflow = {
    id: '1',
    label: 'Test Workflow',
};

const mockUpdateWorkflowMutation = vi.fn().mockResolvedValue(undefined);

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

afterEach(() => {
    queryClient.clear();
});

const renderSettingsMenu = () =>
    render(
        <MemoryRouter>
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <SettingsMenu
                        integration={mockIntegration}
                        updateWorkflowMutation={mockUpdateWorkflowMutation as unknown as UpdateWorkflowMutationType}
                        workflow={mockWorkflow}
                    />
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );

it('opens the settings menu with the Workflow and Integration tabs', async () => {
    renderSettingsMenu();

    expect(screen.queryByLabelText('Workflow tab')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.getByLabelText('Workflow tab')).toBeInTheDocument();
    expect(screen.getByLabelText('Integration tab')).toBeInTheDocument();
});

it('lists the workflow creation entries on the Integration tab', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    expect(screen.getByRole('button', {name: 'New Workflow'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Import Workflow Button'})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Integration History'})).toBeInTheDocument();
});

it('opens the create workflow dialog from the Integration tab and closes the menu', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('button', {name: 'New Workflow'}));

    expect(await screen.findByRole('dialog', {name: 'Create Workflow'})).toBeInTheDocument();
    expect(screen.queryByLabelText('Integration tab')).not.toBeInTheDocument();
});

it('closes the create workflow dialog when cancelled', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('button', {name: 'New Workflow'}));

    await screen.findByRole('dialog', {name: 'Create Workflow'});

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    await waitFor(() => {
        expect(screen.queryByRole('dialog', {name: 'Create Workflow'})).not.toBeInTheDocument();
    });
});

it('closes the delete integration dialog without deleting when cancelled', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('button', {name: 'Delete Integration'}));

    expect(screen.getByRole('alertdialog')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
});

it('closes the delete workflow dialog without deleting when cancelled', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

    expect(screen.getByRole('alertdialog')).toHaveAccessibleDescription(
        'This action cannot be undone. This will permanently delete the workflow.'
    );

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
});
