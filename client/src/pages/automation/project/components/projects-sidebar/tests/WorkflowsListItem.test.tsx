import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowsListItem from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

const mockOnProjectClick = vi.fn();

const mockWorkflow = {
    label: 'Workflow One',
    projectWorkflowId: 1001,
};

const renderWorkflowsListItem = () => {
    render(
        <TooltipProvider>
            <WorkflowsListItem
                calculateTimeDifference={vi.fn().mockReturnValue('1 hour ago')}
                currentWorkflowId="1001"
                findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                onProjectClick={mockOnProjectClick}
                setSelectedProjectId={vi.fn()}
                workflow={mockWorkflow}
            />
        </TooltipProvider>
    );
};

it('should render workflow name', () => {
    renderWorkflowsListItem();

    expect(screen.getByText('Workflow One')).toBeInTheDocument();
});

it('should render workflow last modified date', () => {
    renderWorkflowsListItem();

    expect(screen.getByText('1 hour ago')).toBeInTheDocument();
});

it('should call onProjectClick with correct projectId and selectedProjectId when the workflow is clicked', () => {
    renderWorkflowsListItem();

    fireEvent.click(screen.getByText('Workflow One'));

    expect(mockOnProjectClick).toHaveBeenCalledWith(1050, 1001);
});

it('should render component icons for workflow', () => {
    const mockWorkflowWithComponents = {
        ...mockWorkflow,
        workflowTaskComponentNames: ['Task1', 'Task2', 'Task3', 'Task4'],
        workflowTriggerComponentNames: ['Trigger1'],
    };

    render(
        <TooltipProvider>
            <WorkflowsListItem
                calculateTimeDifference={vi.fn().mockReturnValue('1 hour ago')}
                currentWorkflowId="1001"
                findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                onProjectClick={mockOnProjectClick}
                setSelectedProjectId={vi.fn()}
                workflow={mockWorkflowWithComponents}
            />
        </TooltipProvider>
    );

    expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(5);
});

it('should show +X indicator when there are more than 7 components', () => {
    const mockWorkflowWithComponents = {
        ...mockWorkflow,
        workflowTaskComponentNames: ['Task1', 'Task2', 'Task3', 'Task4', 'Task5', 'Task6', 'Task7', 'Task8'],
        workflowTriggerComponentNames: ['Trigger1'],
    };

    render(
        <TooltipProvider>
            <WorkflowsListItem
                calculateTimeDifference={vi.fn().mockReturnValue('1 hour ago')}
                currentWorkflowId="1001"
                findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                onProjectClick={mockOnProjectClick}
                setSelectedProjectId={vi.fn()}
                workflow={mockWorkflowWithComponents}
            />
        </TooltipProvider>
    );

    expect(screen.getByText('+2')).toBeInTheDocument();
});
