import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowsListItem from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, expect, it, vi} from 'vitest';

const mockOnProjectClick = vi.fn();

const mockProject = {
    id: 1050,
    name: 'Project One',
    workspaceId: 1,
};

const mockWorkflow = {
    label: 'Workflow One',
    projectWorkflowId: 1001,
};

beforeEach(() => {
    vi.clearAllMocks();
});

const renderWorkflowsListItem = () => {
    render(
        <MemoryRouter>
            <TooltipProvider>
                <WorkflowsListItem
                    calculateTimeDifference={vi.fn().mockReturnValue('1 hour ago')}
                    currentWorkflowId="1001"
                    findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                    onProjectClick={mockOnProjectClick}
                    project={mockProject}
                    setSelectedProjectId={vi.fn()}
                    workflow={mockWorkflow}
                />
            </TooltipProvider>
        </MemoryRouter>
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

it('should not select the workflow when the actions menu is used', async () => {
    renderWorkflowsListItem();

    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));

    expect(mockOnProjectClick).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

    expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

    expect(mockOnProjectClick).not.toHaveBeenCalled();
});

it('should render the trigger apart from the task component icons', () => {
    const mockWorkflowWithComponents = {
        ...mockWorkflow,
        workflowTaskComponentNames: ['Task1', 'Task2', 'Task3', 'Task4'],
        workflowTriggerComponentNames: ['Trigger1'],
    };

    render(
        <MemoryRouter>
            <TooltipProvider>
                <WorkflowsListItem
                    calculateTimeDifference={vi.fn().mockReturnValue('1 hour ago')}
                    currentWorkflowId="1001"
                    findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                    onProjectClick={mockOnProjectClick}
                    project={mockProject}
                    setSelectedProjectId={vi.fn()}
                    workflow={mockWorkflowWithComponents}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(4);
});

it('should show +X indicator when there are more task components than fit', () => {
    const mockWorkflowWithComponents = {
        ...mockWorkflow,
        workflowTaskComponentNames: ['Task1', 'Task2', 'Task3', 'Task4', 'Task5', 'Task6', 'Task7', 'Task8'],
        workflowTriggerComponentNames: ['Trigger1'],
    };

    render(
        <MemoryRouter>
            <TooltipProvider>
                <WorkflowsListItem
                    calculateTimeDifference={vi.fn().mockReturnValue('1 hour ago')}
                    currentWorkflowId="1001"
                    findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                    onProjectClick={mockOnProjectClick}
                    project={mockProject}
                    setSelectedProjectId={vi.fn()}
                    workflow={mockWorkflowWithComponents}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

    expect(screen.getByText('+1')).toBeInTheDocument();
});
