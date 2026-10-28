import ProjectList from '@/pages/automation/projects/components/project-list/ProjectList';
import {ProjectListItemTabType} from '@/pages/automation/projects/components/project-list/ProjectListItemTabs';
import {Project} from '@/shared/middleware/automation/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/projects/components/project-list/ProjectListItem', () => ({
    default: ({
        activeTab,
        deploymentsContent,
        onTabChange,
        project,
        workflowsContent,
    }: {
        activeTab?: ProjectListItemTabType;
        deploymentsContent: React.ReactNode;
        onTabChange: (tab?: ProjectListItemTabType, expandedProjectDeploymentId?: number) => void;
        project: Project;
        workflowsContent: React.ReactNode;
    }) => (
        <div>
            <span>{`${project.name} tab ${activeTab ?? 'none'}`}</span>

            <button onClick={() => onTabChange('deployments', 9)}>{`Open ${project.name} deployments`}</button>

            {activeTab === 'deployments' && deploymentsContent}

            {activeTab === 'workflows' && workflowsContent}
        </div>
    ),
}));

vi.mock('@/pages/automation/project/components/ProjectDeploymentsPanel', () => ({
    default: ({expandedProjectDeploymentId, project}: {expandedProjectDeploymentId?: number; project: Project}) => (
        <div>{`${project.name} deployments panel ${expandedProjectDeploymentId ?? 'none'}`}</div>
    ),
}));

vi.mock('@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowList', () => ({
    default: ({project}: {project: Project}) => <div>{`${project.name} workflow list`}</div>,
}));

const projects = [
    {id: 1, name: 'Orders', workspaceId: 1},
    {id: 2, name: 'Leads', workspaceId: 1},
] as Project[];

const CurrentSearch = () => {
    const location = useLocation();

    return <div>{`search: ${location.search}`}</div>;
};

const renderProjectList = (initialEntry: string | {pathname: string; search: string; state: unknown}) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <ProjectList projectGitConfigurations={[]} projects={projects} tags={[]} />

            <CurrentSearch />
        </MemoryRouter>
    );

describe('ProjectList', () => {
    beforeEach(() => {
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('scrolls the project named in the URL into view and drops the one-off params', () => {
        const scrolledElements: Element[] = [];

        Element.prototype.scrollIntoView = vi.fn(function (this: Element) {
            scrolledElements.push(this);
        });

        renderProjectList({
            pathname: '/automation/projects',
            search: '?categoryId=4&projectId=2&tab=deployments',
            state: null,
        });

        expect(scrolledElements).toHaveLength(1);
        expect(scrolledElements[0]).toHaveTextContent('Leads tab deployments');
        expect(screen.getByText('search: ?categoryId=4')).toBeInTheDocument();
        expect(screen.getByText('Leads tab deployments')).toBeInTheDocument();
    });

    it('starts with every row collapsed and no tab selected', () => {
        renderProjectList('/automation/projects');

        expect(screen.getByText('Orders tab none')).toBeInTheDocument();
        expect(screen.getByText('Leads tab none')).toBeInTheDocument();
        expect(screen.queryByText(/deployments panel/)).not.toBeInTheDocument();
    });

    it('opens the deployments tab of the project named in the URL', () => {
        renderProjectList({
            pathname: '/automation/projects',
            search: '?projectId=2&tab=deployments',
            state: {projectDeploymentId: 5},
        });

        expect(screen.getByText('Leads tab deployments')).toBeInTheDocument();
        expect(screen.getByText('Leads deployments panel 5')).toBeInTheDocument();
        expect(screen.getByText('Orders tab none')).toBeInTheDocument();
    });

    it('shows the deployments panel of the tab a row opens', () => {
        renderProjectList('/automation/projects');

        fireEvent.click(screen.getByText('Open Orders deployments'));

        expect(screen.getByText('Orders tab deployments')).toBeInTheDocument();
        expect(screen.getByText('Orders deployments panel 9')).toBeInTheDocument();
    });
});
