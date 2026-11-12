import {TooltipProvider} from '@/components/ui/tooltip';
import SettingsMenu from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    handleImportWorkflow: vi.fn(),
    handleRestoreIntegrationVersion: vi.fn(),
}));

vi.mock(
    '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/hooks/useSettingsMenu',
    async (importOriginal) => {
        const original =
            await importOriginal<
                typeof import('@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/hooks/useSettingsMenu')
            >();

        return {
            useSettingsMenu: (...args: Parameters<typeof original.useSettingsMenu>) => ({
                ...original.useSettingsMenu(...args),
                handleImportWorkflow: hoisted.handleImportWorkflow,
                handleRestoreIntegrationVersion: hoisted.handleRestoreIntegrationVersion,
                restoreIntegrationVersionPending: true,
            }),
        };
    }
);

vi.mock('@/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet', () => ({
    default: ({
        integrationId,
        onClose,
        onRestoreIntegrationVersion,
        restoreIntegrationVersionPending,
    }: {
        integrationId: number;
        onClose: () => void;
        onRestoreIntegrationVersion: (integrationVersion: number, onSuccess: () => void) => void;
        restoreIntegrationVersionPending?: boolean;
    }) => (
        <div aria-label="Integration Version History" role="dialog">
            <span>{`integration ${integrationId}`}</span>

            <span>{restoreIntegrationVersionPending ? 'restore pending' : 'restore idle'}</span>

            <button onClick={() => onRestoreIntegrationVersion(2, onClose)} type="button">
                Restore V2
            </button>
        </div>
    ),
}));

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

    hoisted.handleImportWorkflow.mockReset();
    hoisted.handleRestoreIntegrationVersion.mockReset();
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

it('moves focus through the Workflow tab menu items with the arrow keys', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.keyboard('{ArrowDown}');

    expect(screen.getByRole('menuitem', {name: 'Edit'})).toHaveFocus();

    await userEvent.keyboard('{ArrowDown}');

    expect(screen.getByRole('menuitem', {name: 'Export'})).toHaveFocus();
});

it('lists the workflow actions on the Workflow tab with Delete styled as destructive', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
        'Edit',
        'Export',
        'Delete',
    ]);
    expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
});

it('lists the workflow creation entries on the Integration tab', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    expect(screen.getByRole('menuitem', {name: 'New Workflow'})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: 'Integration History'})).toBeInTheDocument();
});

it('opens the create workflow dialog from the Integration tab and closes the menu', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

    expect(await screen.findByRole('dialog', {name: 'Create Workflow'})).toBeInTheDocument();
    expect(screen.queryByLabelText('Integration tab')).not.toBeInTheDocument();
});

it('closes the create workflow dialog when cancelled', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

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

    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

    expect(screen.getByRole('alertdialog')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
});

it('closes the delete workflow dialog without deleting when cancelled', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

    expect(screen.getByRole('alertdialog')).toHaveAccessibleDescription(
        'This action cannot be undone. This will permanently delete the workflow.'
    );

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
});

it('imports the chosen workflow file after Import Workflow closes the menu', async () => {
    const {container} = renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

    await waitFor(() => {
        expect(screen.queryByLabelText('Integration tab')).not.toBeInTheDocument();
    });

    const workflowFileInput = container.ownerDocument.querySelector('input[type="file"]') as HTMLInputElement;

    await userEvent.upload(
        workflowFileInput,
        new File(['{"label": "Imported"}'], 'workflow.json', {type: 'application/json'})
    );

    await waitFor(() => {
        expect(hoisted.handleImportWorkflow).toHaveBeenCalledWith('{"label": "Imported"}');
    });
});

it('restores a version chosen in the integration history through the settings menu hook', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Integration tab'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'Integration History'}));

    const versionHistorySheet = await screen.findByRole('dialog', {name: 'Integration Version History'});

    expect(versionHistorySheet).toHaveTextContent('integration 1');
    expect(versionHistorySheet).toHaveTextContent('restore pending');

    await userEvent.click(screen.getByRole('button', {name: 'Restore V2'}));

    expect(hoisted.handleRestoreIntegrationVersion).toHaveBeenCalledWith(2, expect.any(Function));
});
