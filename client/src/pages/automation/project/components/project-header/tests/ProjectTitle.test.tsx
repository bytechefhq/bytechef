import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectTitle from '@/pages/automation/project/components/project-header/components/ProjectTitle';
import {Project, ProjectStatus} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockOnProjectValueChange = vi.fn();

const mockProject: Project = {
    id: 1,
    lastProjectVersion: 3,
    lastStatus: ProjectStatus.Draft,
    name: 'Test Project',
    projectWorkflowIds: [11],
    workspaceId: 1,
};

const mockProjects: Project[] = [
    mockProject,
    {
        id: 2,
        lastProjectVersion: 3,
        lastPublishedDate: new Date(),
        lastStatus: ProjectStatus.Draft,
        name: 'Other Project',
        projectWorkflowIds: [22, 23],
        workspaceId: 1,
    },
    {id: 3, name: 'Empty Project', projectWorkflowIds: [], workspaceId: 1},
];

beforeEach(() => {
    vi.clearAllMocks();
});

it('should render the correct project title', () => {
    render(
        <TooltipProvider>
            <ProjectTitle project={mockProject} />
        </TooltipProvider>
    );

    expect(screen.getByText(`Test Project`)).toBeInTheDocument();
    expect(screen.getByText(`V3`)).toBeInTheDocument();
    expect(screen.getByText(`DRAFT`)).toBeInTheDocument();
    expect(screen.queryByLabelText('Project select')).not.toBeInTheDocument();
});

it('lists workspace projects and switches to the selected one', async () => {
    render(
        <TooltipProvider>
            <ProjectTitle
                onProjectValueChange={mockOnProjectValueChange}
                project={mockProject}
                projects={mockProjects}
            />
        </TooltipProvider>
    );

    await userEvent.click(screen.getByLabelText('Project select'));

    expect(screen.getByText('Projects')).toBeInTheDocument();
    expect(screen.getByRole('menuitemradio', {name: 'Test Project'})).toHaveAttribute('data-state', 'checked');
    expect(screen.getByRole('menuitemradio', {name: 'Empty Project'})).toHaveAttribute('data-disabled');

    await userEvent.click(screen.getByRole('menuitemradio', {name: /Other Project/}));

    expect(mockOnProjectValueChange).toHaveBeenCalledWith(2);
});

it('marks projects that have been published with their last published version', async () => {
    render(
        <TooltipProvider>
            <ProjectTitle
                onProjectValueChange={mockOnProjectValueChange}
                project={mockProject}
                projects={mockProjects}
            />
        </TooltipProvider>
    );

    await userEvent.click(screen.getByLabelText('Project select'));

    expect(screen.getByRole('menuitemradio', {name: /Other Project/})).toContainElement(
        screen.getByRole('img', {name: 'Published V2'})
    );
    expect(screen.getAllByRole('img', {name: /Published/})).toHaveLength(1);
});

it('shows the published version in a tooltip when hovering the dot', async () => {
    render(
        <TooltipProvider>
            <ProjectTitle
                onProjectValueChange={mockOnProjectValueChange}
                project={mockProject}
                projects={mockProjects}
            />
        </TooltipProvider>
    );

    await userEvent.click(screen.getByLabelText('Project select'));
    await userEvent.hover(screen.getByRole('img', {name: 'Published V2'}));

    expect(await screen.findByRole('tooltip')).toHaveTextContent('Published V2');
});
