import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectBreadcrumb from '@/pages/automation/project/components/project-header/components/ProjectBreadcrumb';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {expect, it} from 'vitest';

const mockProject = {
    id: 1,
    lastProjectVersion: 2,
    lastStatus: ProjectStatus.Draft,
    name: 'Graph',
    projectWorkflowIds: [11],
    workspaceId: 1,
};

const renderProjectBreadcrumb = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ProjectBreadcrumb
                    itemSelect={<button>graph1</button>}
                    onProjectValueChange={() => {}}
                    project={mockProject}
                    projects={[mockProject]}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

it('links back to the Projects list page first', () => {
    renderProjectBreadcrumb();

    expect(screen.getByRole('link', {name: 'Projects'})).toHaveAttribute('href', '/automation/projects');
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('Projects');
});

it('shows the project select followed by the workflow select', () => {
    renderProjectBreadcrumb();

    const listItems = screen.getAllByRole('listitem');

    expect(listItems).toHaveLength(3);
    expect(listItems[1]).toContainElement(screen.getByLabelText('Project select'));
    expect(listItems[2]).toContainElement(screen.getByRole('button', {name: 'graph1'}));
});
