import {DropdownMenu, DropdownMenuContent} from '@/components/DropdownMenu/DropdownMenu';
import IntegrationTabButtons from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/components/IntegrationTabButtons';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockProps = {
    onDeleteIntegrationClick: vi.fn(),
    onImportWorkflowClick: vi.fn(),
    onNewWorkflowClick: vi.fn(),
    onShowEditIntegrationDialogClick: vi.fn(),
    onShowIntegrationVersionHistorySheet: vi.fn(),
};

const onOpenChange = vi.fn();

const renderIntegrationTabButtons = () =>
    render(
        <DropdownMenu onOpenChange={onOpenChange} open>
            <DropdownMenuContent>
                <IntegrationTabButtons {...mockProps} />
            </DropdownMenuContent>
        </DropdownMenu>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

it('lists the integration actions as menu items in order', () => {
    renderIntegrationTabButtons();

    expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
        'Edit',
        'New Workflow',
        'Import Workflow',
        'Integration History',
        'Delete',
    ]);
    expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
});

it('opens the new workflow dialog and closes the menu when New Workflow is selected', async () => {
    renderIntegrationTabButtons();

    await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

    expect(mockProps.onNewWorkflowClick).toHaveBeenCalled();
    expect(onOpenChange).toHaveBeenCalledWith(false);
});

it('asks for a workflow file and closes the menu when Import Workflow is selected', async () => {
    renderIntegrationTabButtons();

    await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

    expect(mockProps.onImportWorkflowClick).toHaveBeenCalled();
    expect(onOpenChange).toHaveBeenCalledWith(false);
});

it('keeps only the integration actions for a code workflow', () => {
    render(
        <DropdownMenu onOpenChange={onOpenChange} open>
            <DropdownMenuContent>
                <IntegrationTabButtons {...mockProps} codeWorkflow />
            </DropdownMenuContent>
        </DropdownMenu>
    );

    expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
        'Edit',
        'Integration History',
        'Delete',
    ]);
});
