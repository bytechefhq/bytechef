import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectWorkflowsList from '@/pages/automation/project/components/projects-sidebar/components/ProjectWorkflowsList';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

const mockProject: Project = {
    id: 1050,
    lastPublishedDate: new Date('2025-03-03T12:38:37Z'),
    lastStatus: 'DRAFT',
    lastVersion: 5,
    name: 'Project 1',
    projectWorkflowIds: [1001, 1002],
    workspaceId: 1000,
};

const mockFilteredWorkflowsList: Workflow[] = [
    {
        label: 'Workflow One',
        projectWorkflowId: 1001,
    },
    {
        label: 'Workflow Two',
        projectWorkflowId: 1002,
    },
];

const renderProjectWorkflowsList = (mockUnpublishedProject?: Project) => {
    render(
        <TooltipProvider>
            <ProjectWorkflowsList
                calculateTimeDifference={vi.fn()}
                currentWorkflowId="1001"
                filteredWorkflowsList={mockFilteredWorkflowsList}
                findProjectIdByWorkflow={vi.fn().mockReturnValue(1050)}
                onProjectClick={vi.fn()}
                project={mockUnpublishedProject ?? mockProject}
                setSelectedProjectId={vi.fn()}
            />
        </TooltipProvider>
    );
};

it('should render project name and workflows list', () => {
    renderProjectWorkflowsList();

    expect(screen.getByText('Project 1')).toBeInTheDocument();

    expect(screen.getByText('Workflow One')).toBeInTheDocument();
    expect(screen.getByText('Workflow Two')).toBeInTheDocument();
});

it('should show a "PUBLISHED" badge for a published project', () => {
    renderProjectWorkflowsList();

    expect(screen.getByText('V4')).toBeInTheDocument();
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
});

it('should show a "DRAFT" badge for an unpublished project', () => {
    const mockUnpublishedProject: Project = {
        id: 1050,
        lastPublishedDate: undefined,
        lastStatus: 'DRAFT',
        lastVersion: 2,
        name: 'Project 1',
        projectWorkflowIds: [1001, 1002],
        workspaceId: 1000,
    };
    renderProjectWorkflowsList(mockUnpublishedProject);

    expect(screen.getByText('V2')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
});
