import {TooltipProvider} from '@/components/ui/tooltip';
import SettingsMenu from '@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, expect, it, vi} from 'vitest';

screen.debug();

const mockProject = {
    id: 1,
    name: 'Test Project',
    workspaceId: 1,
};

const mockWorkflow = {
    id: '1',
    label: 'Test Workflow',
};

const mockUpdateWorkflowMutation = vi.fn().mockResolvedValue(undefined);

const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

const queryClient = createTestQueryClient();

afterEach(() => {
    queryClient.clear();
});

const renderSettingsMenu = () => {
    render(
        <MemoryRouter>
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <Routes>
                        <Route
                            element={
                                <SettingsMenu
                                    project={mockProject}
                                    updateWorkflowMutation={
                                        mockUpdateWorkflowMutation as unknown as UpdateWorkflowMutationType
                                    }
                                    workflow={mockWorkflow}
                                />
                            }
                            path="/"
                        />

                        <Route
                            element={<div>Project templates</div>}
                            path="/automation/projects/:projectId/templates"
                        />
                    </Routes>
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );
};

it('should open the settings dropdown menu on click of a settings button', async () => {
    renderSettingsMenu();

    expect(screen.getByLabelText('Settings')).toBeInTheDocument();

    expect(screen.queryByLabelText('Workflow tab')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Settings'));

    await waitFor(() => {
        expect(screen.getByText('Edit')).toBeInTheDocument();

        expect(screen.getByLabelText('Workflow tab')).toBeInTheDocument();

        expect(screen.getByLabelText('Project tab')).toBeInTheDocument();
    });
});

it('should open project tab on click', async () => {
    renderSettingsMenu();

    expect(screen.queryByLabelText('Workflow tab')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.queryByText('Project History')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Project tab'));

    await waitFor(() => {
        expect(screen.getByText('Project History')).toBeInTheDocument();
    });
});

it('should close the dropdown on click of a button inside the Workflow tab', async () => {
    renderSettingsMenu();

    expect(screen.queryByLabelText('Workflow tab')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.getByLabelText('Workflow tab')).toBeInTheDocument();

    await userEvent.click(screen.getByText('Export'));

    await waitFor(() => {
        expect(screen.queryByLabelText('Workflow tab')).not.toBeInTheDocument();
    });
});

it('should close the dropdown on click of a button inside the Project tab', async () => {
    renderSettingsMenu();

    expect(screen.queryByLabelText('Project tab')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.getByLabelText('Project tab')).toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Project tab'));

    await userEvent.click(screen.getByText('Project History'));

    await waitFor(() => {
        expect(screen.queryByLabelText('Project tab')).not.toBeInTheDocument();
    });
});

it('should close the delete workflow dialog without deleting when cancelled', async () => {
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

it('should close the delete project dialog without deleting when cancelled', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Project tab'));

    await userEvent.click(screen.getByRole('button', {name: 'Delete Project'}));

    expect(screen.getByRole('button', {name: 'Confirm Project Deletion'})).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    await waitFor(() => {
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
});

it('should open the create workflow dialog from the Project tab', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Project tab'));

    await userEvent.click(screen.getByRole('button', {name: 'New Workflow'}));

    expect(await screen.findByRole('dialog', {name: 'Create Workflow'})).toBeInTheDocument();
});

it('should open the project templates page from the Project tab', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Project tab'));

    await userEvent.click(screen.getByRole('button', {name: 'New Workflow from Template'}));

    expect(await screen.findByText('Project templates')).toBeInTheDocument();
});

it('should open the workflow file picker from the Project tab', async () => {
    const inputClickSpy = vi.spyOn(HTMLInputElement.prototype, 'click').mockImplementation(() => {});

    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Project tab'));

    await userEvent.click(screen.getByRole('button', {name: 'Import Workflow'}));

    expect(inputClickSpy).toHaveBeenCalledTimes(1);
    expect((inputClickSpy.mock.contexts[0] as HTMLInputElement).accept).toBe('.json,.yaml,.yml');

    inputClickSpy.mockRestore();
});
