import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowSelect from '@/pages/automation/project/components/project-header/components/WorkflowSelect';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

screen.debug();

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

it('should open the select menu on click', () => {
    renderWorkflowSelect();

    expect(screen.queryByText('Workflows')).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Workflow select'));

    expect(screen.getByText('Workflows')).toBeInTheDocument();
});

it('should show the other workflow once the menu is open', () => {
    renderWorkflowSelect();

    expect(screen.queryByText('Worflow 2')).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Workflow select'));

    expect(screen.getByText('Workflow 2')).toBeInTheDocument();
});

it('should call the onValueChange function with correct workflowId on click', async () => {
    renderWorkflowSelect();

    expect(screen.queryByText('Worflow 2')).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Workflow select'));

    fireEvent.click(screen.getByText('Workflow 2'));

    expect(mockOnValueChange).toHaveBeenCalledWith(2222);
});
