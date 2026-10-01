import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectItemSelect from '@/pages/automation/project/components/project-header/components/ProjectItemSelect';
import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, expect, it, vi} from 'vitest';

const mockNavigate = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({
        agents: [
            {id: 'agent-1', projectId: '5', title: 'Support Bot'},
            {id: 'agent-2', projectId: '5', title: 'Billing Bot'},
            {id: 'agent-3', projectId: '8', title: 'Other Project Bot'},
            {id: '1050', projectId: '5', title: 'Same Id Agent'},
        ],
    }),
}));

const mockOnWorkflowValueChange = vi.fn();

const mockProjectWorkflows = [
    {label: 'Workflow 1', projectWorkflowId: 1111},
    {label: 'Workflow 2', projectWorkflowId: 2222},
];

const renderProjectItemSelect = (props: Partial<React.ComponentProps<typeof ProjectItemSelect>> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ProjectItemSelect
                    currentLabel="Workflow 1"
                    currentProjectWorkflowId={1111}
                    onWorkflowValueChange={mockOnWorkflowValueChange}
                    projectId={5}
                    projectWorkflows={mockProjectWorkflows}
                    {...props}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

it('shows the closed select with the current item label', () => {
    renderProjectItemSelect();

    expect(screen.getByLabelText('Project item select')).toBeInTheDocument();
    expect(screen.getByText('Workflow 1')).toBeInTheDocument();
});

it('lists the Workflows and Agents groups scoped to the current project', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));

    expect(screen.getByText('Workflows')).toBeInTheDocument();
    expect(screen.getByText('Workflow 2')).toBeInTheDocument();

    expect(screen.getByText('Agents')).toBeInTheDocument();
    expect(screen.getByText('Support Bot')).toBeInTheDocument();
    expect(screen.getByText('Billing Bot')).toBeInTheDocument();
    expect(screen.queryByText('Other Project Bot')).not.toBeInTheDocument();
});

it('shows a workflow icon and an agent icon next to each group item', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));

    const menu = screen.getByRole('menu');

    const workflowItem = within(menu).getByText('Workflow 1').closest('[role="menuitemradio"]') as HTMLElement;
    const agentItem = within(menu).getByText('Support Bot').closest('[role="menuitemradio"]') as HTMLElement;

    expect(within(workflowItem).getByText('Workflow 1').previousElementSibling).toHaveClass(
        'lucide-workflow',
        'size-4',
        'shrink-0'
    );
    expect(within(agentItem).getByText('Support Bot').previousElementSibling).toHaveClass(
        'lucide-bot',
        'size-4',
        'shrink-0'
    );
});

it('omits the Agents group header when the project has no agents', async () => {
    renderProjectItemSelect({projectId: 999});

    await userEvent.click(screen.getByLabelText('Project item select'));

    expect(screen.queryByText('Agents')).not.toBeInTheDocument();
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

it('marks the current agent as the checked menu item', async () => {
    renderProjectItemSelect({
        currentAgentId: 'agent-1',
        currentLabel: 'Support Bot',
        currentProjectWorkflowId: undefined,
    });

    await userEvent.click(screen.getByLabelText('Project item select'));

    const menu = screen.getByRole('menu');

    expect(within(menu).getByText('Support Bot').closest('[role="menuitemradio"]')).toHaveAttribute(
        'aria-checked',
        'true'
    );
});

it('navigates to a project-workflow route when a workflow is selected', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));
    await userEvent.click(screen.getByText('Workflow 2'));

    expect(mockOnWorkflowValueChange).toHaveBeenCalledWith(2222);
});

it('navigates to the agent path when an agent is selected', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));
    await userEvent.click(screen.getByText('Billing Bot'));

    expect(mockNavigate).toHaveBeenCalledWith('/automation/projects/5/agents/agent-2');
});

it('dispatches the project-workflow whose id collides with an agent id to onWorkflowValueChange', async () => {
    renderProjectItemSelect({
        projectWorkflows: [...mockProjectWorkflows, {label: 'Workflow 1050', projectWorkflowId: 1050}],
    });

    const navigateCallsBefore = mockNavigate.mock.calls.length;

    await userEvent.click(screen.getByLabelText('Project item select'));
    await userEvent.click(screen.getByText('Workflow 1050'));

    expect(mockOnWorkflowValueChange).toHaveBeenCalledWith(1050);
    expect(mockNavigate.mock.calls.length).toBe(navigateCallsBefore);
});

it('dispatches the agent whose id collides with a project-workflow id to the agent path', async () => {
    renderProjectItemSelect({
        projectWorkflows: [...mockProjectWorkflows, {label: 'Workflow 1050', projectWorkflowId: 1050}],
    });

    const onWorkflowValueChangeCallsBefore = mockOnWorkflowValueChange.mock.calls.length;

    await userEvent.click(screen.getByLabelText('Project item select'));
    await userEvent.click(screen.getByText('Same Id Agent'));

    expect(mockNavigate).toHaveBeenCalledWith('/automation/projects/5/agents/1050');
    expect(mockOnWorkflowValueChange.mock.calls.length).toBe(onWorkflowValueChangeCallsBefore);
});

it('renders with the design-system DropdownMenu', async () => {
    renderProjectItemSelect();

    await userEvent.click(screen.getByLabelText('Project item select'));

    expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
    expect(screen.getByRole('menuitemradio', {name: 'Workflow 2'})).toHaveClass('text-content-neutral-primary');
});
