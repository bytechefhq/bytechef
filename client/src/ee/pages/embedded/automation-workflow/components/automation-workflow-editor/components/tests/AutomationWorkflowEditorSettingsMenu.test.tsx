import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorSettingsMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorSettingsMenu';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockHandlers = {
    onDeleteProjectClick: vi.fn(),
    onDeleteWorkflowClick: vi.fn(),
    onDuplicateProjectClick: vi.fn(),
    onDuplicateWorkflowClick: vi.fn(),
    onEditProjectClick: vi.fn(),
    onEditWorkflowClick: vi.fn(),
    onExportProjectClick: vi.fn(),
    onExportWorkflowClick: vi.fn(),
    onImportWorkflowClick: vi.fn(),
    onNewWorkflowClick: vi.fn(),
    onProjectHistoryClick: vi.fn(),
};

const renderSettingsMenu = () =>
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorSettingsMenu {...mockHandlers} />
        </TooltipProvider>
    );

const openProjectTab = async () => {
    await userEvent.click(screen.getByLabelText('Settings'));

    await userEvent.click(screen.getByLabelText('Project tab'));
};

beforeEach(() => {
    vi.clearAllMocks();
});

it('opens the settings menu with the Workflow and Project tabs', async () => {
    renderSettingsMenu();

    expect(screen.queryByLabelText('Workflow tab')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.getByLabelText('Workflow tab')).toBeInTheDocument();
    expect(screen.getByLabelText('Project tab')).toBeInTheDocument();
});

it('lists the workflow actions on the Workflow tab', async () => {
    renderSettingsMenu();

    await userEvent.click(screen.getByLabelText('Settings'));

    expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: 'Duplicate'})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: 'Export'})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', {name: 'New Workflow'})).not.toBeInTheDocument();
});

it('lists the workflow creation entries on the Project tab between Export and Project History', async () => {
    renderSettingsMenu();

    await openProjectTab();

    const menuItemNames = screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent);

    expect(menuItemNames).toEqual([
        'Edit',
        'Duplicate',
        'Export',
        'New Workflow',
        'Import Workflow',
        'Project History',
        'Delete',
    ]);
});

it('starts a new workflow from the Project tab and closes the menu', async () => {
    renderSettingsMenu();

    await openProjectTab();

    await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

    expect(mockHandlers.onNewWorkflowClick).toHaveBeenCalledTimes(1);
    expect(screen.queryByLabelText('Project tab')).not.toBeInTheDocument();
});

it('starts a workflow import from the Project tab and closes the menu', async () => {
    renderSettingsMenu();

    await openProjectTab();

    await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

    expect(mockHandlers.onImportWorkflowClick).toHaveBeenCalledTimes(1);
    expect(screen.queryByLabelText('Project tab')).not.toBeInTheDocument();
});

it('calls the project handlers from the Project tab', async () => {
    renderSettingsMenu();

    await openProjectTab();

    await userEvent.click(screen.getByRole('menuitem', {name: 'Project History'}));

    expect(mockHandlers.onProjectHistoryClick).toHaveBeenCalledTimes(1);

    await openProjectTab();

    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

    expect(mockHandlers.onDeleteProjectClick).toHaveBeenCalledTimes(1);
    expect(mockHandlers.onDeleteWorkflowClick).not.toHaveBeenCalled();
});
