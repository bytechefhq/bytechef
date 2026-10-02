import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectItemSelect from '@/pages/automation/project/components/project-header/components/ProjectItemSelect';
import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockOnWorkflowValueChange = vi.fn();

const mockProjectWorkflows = [
    {label: 'Workflow 1', projectWorkflowId: 1111},
    {label: 'Workflow 2', projectWorkflowId: 2222},
];

const renderProjectItemSelect = (props: Partial<React.ComponentProps<typeof ProjectItemSelect>> = {}) =>
    render(
        <TooltipProvider>
            <ProjectItemSelect
                currentLabel="Workflow 1"
                currentProjectWorkflowId={1111}
                onWorkflowValueChange={mockOnWorkflowValueChange}
                projectWorkflows={mockProjectWorkflows}
                {...props}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

it('shows the closed select with the current workflow label', () => {
    renderProjectItemSelect();

    expect(screen.getByLabelText('Project item select')).toBeInTheDocument();
    expect(screen.getByText('Workflow 1')).toBeInTheDocument();
    expect(screen.queryByText('Workflow 2')).not.toBeInTheDocument();
});

it('lists the project workflows under a Workflows group once opened', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));

    expect(screen.getByText('Workflows')).toBeInTheDocument();
    expect(screen.getByText('Workflow 2')).toBeInTheDocument();
});

it('omits the Workflows group header when the project has no workflows', async () => {
    renderProjectItemSelect({projectWorkflows: []});

    await userEvent.click(screen.getByLabelText('Project item select'));

    expect(screen.queryByText('Workflows')).not.toBeInTheDocument();
});

it('marks the current workflow as the checked menu item', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));

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

it('calls onWorkflowValueChange with the selected project workflow id', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));
    await userEvent.click(screen.getByText('Workflow 2'));

    expect(mockOnWorkflowValueChange).toHaveBeenCalledWith(2222);
});

it('shows a skeleton while the current workflow label is loading', () => {
    renderProjectItemSelect({currentLabel: undefined});

    expect(screen.queryByText('Workflow 1')).not.toBeInTheDocument();
});
