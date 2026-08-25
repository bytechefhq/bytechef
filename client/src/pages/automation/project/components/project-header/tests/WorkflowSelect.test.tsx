import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowSelect from '@/pages/automation/project/components/project-header/components/WorkflowSelect';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

const mockOnValueChange = vi.fn();

const mockProjectWorkflows = [
    {label: 'Workflow 1', projectWorkflowId: 1111},
    {label: 'Workflow 2', projectWorkflowId: 2222},
];

const renderWorkflowSelect = () => {
    render(
        <TooltipProvider>
            <WorkflowSelect
                currentWorkflowLabel="Workflow 1"
                onValueChange={mockOnValueChange}
                projectId={5}
                projectWorkflowId={1111}
                projectWorkflows={mockProjectWorkflows}
            />
        </TooltipProvider>
    );
};

it('should render the closed workflow select with current workflow as value', () => {
    renderWorkflowSelect();

    expect(screen.getByLabelText('Workflow select')).toBeInTheDocument();

    expect(screen.getByText('Workflow 1')).toBeInTheDocument();

    expect(screen.queryByText('Workflow 2')).not.toBeInTheDocument();
});

it('should open the select menu on click', async () => {
    renderWorkflowSelect();

    expect(screen.queryByText('Workflows')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Workflow select'));

    expect(screen.getByText('Workflows')).toBeInTheDocument();
});

it('should show the other workflow once the menu is open', async () => {
    renderWorkflowSelect();

    expect(screen.queryByText('Workflow 2')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Workflow select'));

    expect(screen.getByText('Workflow 2')).toBeInTheDocument();
});

it('should mark the current workflow as the checked menu item', async () => {
    renderWorkflowSelect();

    await userEvent.click(screen.getByLabelText('Workflow select'));

    const menuItems = screen.getAllByRole('menuitemradio');

    expect(menuItems[0]).toHaveAttribute('aria-checked', 'true');

    expect(menuItems[1]).toHaveAttribute('aria-checked', 'false');
});

it('should call the onValueChange function with correct workflowId on click', async () => {
    renderWorkflowSelect();

    expect(screen.queryByText('Workflow 2')).not.toBeInTheDocument();

    await userEvent.click(screen.getByLabelText('Workflow select'));

    await userEvent.click(screen.getByText('Workflow 2'));

    expect(mockOnValueChange).toHaveBeenCalledWith(2222);
});
