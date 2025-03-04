import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectSelect from '@/pages/automation/project/components/projects-sidebar/components/ProjectSelect';
import {cleanup, fireEvent, render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

const mockProjects = [
    {id: 1050, name: 'Project 1', workspaceId: 1000},
    {id: 1051, name: 'Project 2', workspaceId: 1000},
    {id: 1052, name: 'Project 3', workspaceId: 1000},
];

const mockSetSelectedProjectId = vi.fn();

const renderProjectSelect = (projectId: number, selectedProjectId: number) => {
    render(
        <TooltipProvider>
            <ProjectSelect
                projectId={projectId}
                projects={mockProjects}
                selectedProjectId={selectedProjectId}
                setSelectedProjectId={mockSetSelectedProjectId}
            />
        </TooltipProvider>
    );
};

it('should show "Current project" as selected value when the projectId is equal to selectedProjectId', () => {
    renderProjectSelect(1050, 1050);

    expect(screen.getByText('Current project')).toBeInTheDocument();

    expect(screen.queryByText('Project 1')).not.toBeInTheDocument();
});

it('should open the select dropdown when the select trigger is clicked', () => {
    renderProjectSelect(1050, 1050);

    expect(screen.getByText('Current project')).toBeInTheDocument();

    expect(screen.queryByText('All projects')).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Select project'));

    expect(screen.getByLabelText('Select project')).toHaveAttribute('aria-expanded', 'true');

    expect(screen.getByText('All projects')).toBeInTheDocument();
});

it('should show the list of projects in the select dropdown', () => {
    renderProjectSelect(1050, 1050);

    expect(screen.queryByText('Project 1')).not.toBeInTheDocument();

    expect(screen.queryByText('Project 2')).not.toBeInTheDocument();

    expect(screen.queryByText('Project 3')).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Select project'));

    expect(screen.getByText('Project 1')).toBeInTheDocument();

    expect(screen.getByText('Project 2')).toBeInTheDocument();

    expect(screen.getByText('Project 3')).toBeInTheDocument();
});

it('should show correct select value text based on selectedProjectId values', () => {
    renderProjectSelect(1050, 1050);

    expect(screen.getByText('Current project')).toBeInTheDocument();
    expect(screen.queryByText('All projects')).not.toBeInTheDocument();

    cleanup();

    renderProjectSelect(1050, 0);

    expect(screen.getByText('All projects')).toBeInTheDocument();
    expect(screen.queryByText('Current project')).not.toBeInTheDocument();

    cleanup();

    renderProjectSelect(1050, 1052);

    expect(screen.getByText('Project 3')).toBeInTheDocument();
    expect(screen.queryByText('Current project')).not.toBeInTheDocument();
    expect(screen.queryByText('All projects')).not.toBeInTheDocument();
});

it('should call setSelectedProjectId with "0" when "All projects" button is clicked', () => {
    renderProjectSelect(1050, 1050);

    expect(screen.getByText('Current project')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Select project'));

    expect(screen.getByText('All projects')).toBeInTheDocument();

    fireEvent.click(screen.getByText('All projects'));

    expect(mockSetSelectedProjectId).toHaveBeenCalledWith(0);
});

it('should call setSelectedProjectId with the correct projectId when a project is clicked', () => {
    renderProjectSelect(1050, 1050);

    expect(screen.getByText('Current project')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Select project'));

    expect(screen.getByLabelText('Project 3')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Project 3'));

    expect(mockSetSelectedProjectId).toHaveBeenCalledWith(1052);
});

it('should show both current project and selected project as checked when current project is selected', () => {
    renderProjectSelect(1050, 1050);

    fireEvent.click(screen.getByLabelText('Select project'));

    expect(screen.getByLabelText('Current project')).toHaveAttribute('data-state', 'checked');

    expect(screen.getByLabelText('Project 1')).toHaveAttribute('data-state', 'checked');

    expect(screen.getByLabelText('Project 2')).not.toHaveAttribute('data-state', 'checked');
});

it('should show the selected project as checked in the select dropdown', async () => {
    renderProjectSelect(1050, 1052);

    fireEvent.click(screen.getByLabelText('Select project'));

    expect(screen.getByLabelText('Project 1')).not.toHaveAttribute('data-state', 'checked');

    expect(screen.getByLabelText('Project 3')).toHaveAttribute('data-state', 'checked');
});
