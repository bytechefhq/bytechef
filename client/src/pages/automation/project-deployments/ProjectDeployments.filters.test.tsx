import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectDeployments from '@/pages/automation/project-deployments/ProjectDeployments';
import {render, screen} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

interface FixtureTriggerI {
    type: string;
}

interface FixtureAgentDeploymentI {
    agentId: string;
    id: string;
    workflows: {triggers: FixtureTriggerI[]}[];
}

const hoisted = vi.hoisted(() => ({
    agentDeployments: [] as FixtureAgentDeploymentI[],
    projectDeployments: [] as {id: number; name: string; project: object; projectId: number}[],
    projects: [] as {id: number; name: string}[],
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useAgentDeployments', () => ({
    default: () => ({agentDeployments: hoisted.agentDeployments, agentDeploymentsIsLoading: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => ({
        ai: {copilot: {enabled: true}},
        analytics: {enabled: false, postHog: {apiKey: '', host: ''}},
        application: {edition: 'CE'},
        featureFlags: {},
    }),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: hoisted.projects, error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    ProjectDeploymentKeys: {projectDeployments: ['projectDeployments']},
    useGetWorkspaceProjectDeploymentsQuery: () => ({
        data: hoisted.projectDeployments,
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/automation/projectDeploymentTags.queries', () => ({
    useGetProjectDeploymentTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/components/copilot/CopilotButton', () => ({
    default: () => null,
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet',
    () => ({default: () => null})
);

vi.mock('./components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => null,
}));

vi.mock('./components/project-deployment-list/ProjectDeploymentList', () => ({
    default: ({
        defaultActiveTab,
        projectDeployments,
    }: {
        defaultActiveTab?: string;
        projectDeployments: {id: number; name: string}[];
    }) => (
        <ul data-default-active-tab={defaultActiveTab} data-testid="project-deployment-list">
            {projectDeployments.map((projectDeployment) => (
                <li key={projectDeployment.id}>{projectDeployment.name}</li>
            ))}
        </ul>
    ),
}));

const renderProjectDeployments = (initialEntries: string[] = ['/']) =>
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
                <TooltipProvider>
                    <ProjectDeployments />
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );

const getFilterLink = (name: string, hrefIncludes: string) =>
    screen.getAllByRole('link', {name}).find((link) => link.getAttribute('href')?.includes(hrefIncludes))!;

describe('ProjectDeployments agents filter', () => {
    beforeEach(() => {
        const project = {id: 1, name: 'Sales'};

        hoisted.projects = [project];

        hoisted.projectDeployments = [
            {id: 10, name: 'Scheduled Deployment', project, projectId: 1},
            {id: 11, name: 'Chat Deployment', project, projectId: 1},
            {id: 12, name: 'Workflow Deployment', project, projectId: 1},
        ];

        hoisted.agentDeployments = [
            {agentId: 'a1', id: '10', workflows: [{triggers: [{type: 'schedule/v1/cron'}]}]},
            {agentId: 'a2', id: '11', workflows: [{triggers: [{type: 'chat/v1/newChatRequest'}]}]},
        ];
    });

    it('shows the Agents section between Projects and Tags', () => {
        renderProjectDeployments();

        const projectsHeading = screen.getByText('Projects');
        const agentsHeading = screen.getByText('Agents');
        const tagsHeading = screen.getByText('Tags');

        expect(projectsHeading.compareDocumentPosition(agentsHeading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(agentsHeading.compareDocumentPosition(tagsHeading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

        expect(screen.getByRole('link', {name: 'All Agents'})).toHaveAttribute('href', '/?agents=all');
        expect(getFilterLink('Scheduled', 'agents=scheduled')).toHaveAttribute('href', '/?agents=scheduled');
    });

    it('lists every deployment and opens rows on Workflows without the filter', () => {
        renderProjectDeployments();

        expect(screen.getByText('Scheduled Deployment')).toBeInTheDocument();
        expect(screen.getByText('Chat Deployment')).toBeInTheDocument();
        expect(screen.getByText('Workflow Deployment')).toBeInTheDocument();
        expect(screen.getByTestId('project-deployment-list')).toHaveAttribute('data-default-active-tab', 'workflows');
    });

    it('keeps only deployments with an agent for All Agents, opening rows on Agents', () => {
        renderProjectDeployments(['/?agents=all']);

        expect(screen.getByText('Scheduled Deployment')).toBeInTheDocument();
        expect(screen.getByText('Chat Deployment')).toBeInTheDocument();
        expect(screen.queryByText('Workflow Deployment')).not.toBeInTheDocument();
        expect(screen.getByTestId('project-deployment-list')).toHaveAttribute('data-default-active-tab', 'agents');
        expect(screen.getByText('Agents: All Agents')).toBeInTheDocument();
    });

    it('keeps only deployments with a scheduled agent for Scheduled', () => {
        renderProjectDeployments(['/?agents=scheduled']);

        expect(screen.getByText('Scheduled Deployment')).toBeInTheDocument();
        expect(screen.queryByText('Chat Deployment')).not.toBeInTheDocument();
        expect(screen.queryByText('Workflow Deployment')).not.toBeInTheDocument();
        expect(screen.getByTestId('project-deployment-list')).toHaveAttribute('data-default-active-tab', 'agents');
    });

    it('matches Scheduled off a schedule trigger deployed alongside other triggers on the same workflow', () => {
        hoisted.agentDeployments = [
            {
                agentId: 'a1',
                id: '10',
                workflows: [{triggers: [{type: 'chat/v1/newChatRequest'}, {type: 'schedule/v1/cron'}]}],
            },
        ];

        renderProjectDeployments(['/?agents=scheduled']);

        expect(screen.getByText('Scheduled Deployment')).toBeInTheDocument();
    });

    it('excludes a deployment from Scheduled whose deployed trigger type only resembles the schedule component', () => {
        hoisted.agentDeployments = [{agentId: 'a2', id: '11', workflows: [{triggers: [{type: 'scheduler/v1/cron'}]}]}];

        renderProjectDeployments(['/?agents=scheduled']);

        expect(screen.queryByText('Chat Deployment')).not.toBeInTheDocument();
    });

    it('reports a filter miss when no deployment has a matching agent', () => {
        hoisted.agentDeployments = [];

        renderProjectDeployments(['/?agents=all']);

        expect(screen.getByText('No Matching Project Deployments')).toBeInTheDocument();
    });

    it('combines with the project filter in both directions', () => {
        renderProjectDeployments(['/?projectId=1&agents=all']);

        expect(screen.getByRole('link', {name: 'All Agents'})).toHaveAttribute('href', '/?projectId=1');
        expect(getFilterLink('Scheduled', 'agents=scheduled')).toHaveAttribute(
            'href',
            '/?projectId=1&agents=scheduled'
        );
        expect(screen.getByRole('link', {name: 'Sales'})).toHaveAttribute('href', '/?projectId=1&agents=all');
        expect(screen.getByRole('link', {name: 'All Projects'})).toHaveAttribute('href', '/?agents=all');
    });
});
