import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import posthog from 'posthog-js';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionsEmptyState from './WorkflowExecutionsEmptyState';

interface QueryResultI<T> {
    data?: T;
    error: Error | null;
    isLoading: boolean;
}

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    projectDeploymentsResult: {data: [], error: null, isLoading: false} as QueryResultI<ProjectDeployment[]>,
    projectsResult: {data: [], error: null, isLoading: false} as QueryResultI<Project[]>,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => hoisted.projectsResult,
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => hoisted.projectDeploymentsResult,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition: string}}) => unknown) =>
        selector({application: {edition: 'CE'}}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: undefined}),
}));

const publishedProject: Project = {
    id: 4,
    lastPublishedDate: new Date('2026-10-01T10:00:00Z'),
    name: 'Order alerts',
    projectWorkflowIds: [41],
    workspaceId: 1,
};

const renderEmptyState = (handleRefresh = vi.fn()) =>
    render(
        <MemoryRouter>
            <WorkflowExecutionsEmptyState onRefresh={handleRefresh} />
        </MemoryRouter>
    );

const getCurrentStep = () =>
    within(screen.getByRole('list', {name: 'Getting started'}))
        .getAllByRole('listitem')
        .find((item) => item.getAttribute('aria-current') === 'step')!;

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.projectDeploymentsResult = {data: [], error: null, isLoading: false};
    hoisted.projectsResult = {data: [], error: null, isLoading: false};
});

describe('WorkflowExecutionsEmptyState', () => {
    it('lists the steps to a first run and offers the first one to a new user', () => {
        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'No workflow runs yet'})).toBeInTheDocument();
        expect(getCurrentStep()).toHaveTextContent('Create a project');
        expect(within(getCurrentStep()).getByRole('button', {name: 'Go to Projects'})).toBeInTheDocument();
    });

    it('sends a user with a published project to Deployments and records the click', async () => {
        hoisted.projectsResult = {data: [publishedProject], error: null, isLoading: false};

        renderEmptyState();

        expect(getCurrentStep()).toHaveTextContent('Deploy and enable');

        await userEvent.click(within(getCurrentStep()).getByRole('button', {name: 'Go to Deployments'}));

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/deployments');

        await waitFor(() =>
            expect(posthog.capture).toHaveBeenCalledWith('empty_state_action_clicked', {
                action: 'go_to_deployments',
                page: 'executions',
                stage: 'deploy',
            })
        );
    });

    it('asks to enable a workflow when the deployment is on but none of its workflows are', () => {
        hoisted.projectsResult = {data: [publishedProject], error: null, isLoading: false};
        hoisted.projectDeploymentsResult = {
            data: [
                {
                    enabled: true,
                    id: 7,
                    name: 'Order alerts',
                    projectDeploymentWorkflows: [{enabled: false, workflowId: 'w1'}],
                    projectId: 4,
                },
            ],
            error: null,
            isLoading: false,
        };

        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'Enable a deployed workflow'})).toBeInTheDocument();
        expect(screen.getByText(/A workflow runs when both it and its deployment are enabled/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Go to Deployments'})).toBeInTheDocument();
    });

    it('waits for the first trigger once a deployed workflow is enabled and lets the user refresh', async () => {
        const handleRefresh = vi.fn();

        hoisted.projectsResult = {data: [publishedProject], error: null, isLoading: false};
        hoisted.projectDeploymentsResult = {
            data: [
                {
                    enabled: true,
                    id: 8,
                    name: 'Order alerts',
                    projectDeploymentWorkflows: [{enabled: true, workflowId: 'w1'}],
                    projectId: 4,
                },
            ],
            error: null,
            isLoading: false,
        };

        renderEmptyState(handleRefresh);

        expect(screen.getByRole('heading', {level: 2, name: 'Waiting for the first run'})).toBeInTheDocument();
        expect(screen.getByText(/Test runs from the workflow editor aren't listed here\./)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Refresh'}));

        expect(handleRefresh).toHaveBeenCalledTimes(1);
    });

    it('links to the executions docs', () => {
        renderEmptyState();

        expect(screen.getByRole('link', {name: 'How executions work'})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/platform/automation/monitor/workflow-executions'
        );
    });

    it('shows no state until the journey is known', () => {
        hoisted.projectsResult = {data: undefined, error: null, isLoading: true};

        renderEmptyState();

        expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    it('explains where runs come from without steps when the journey cannot be loaded', () => {
        hoisted.projectsResult = {data: undefined, error: new Error('Request failed'), isLoading: false};

        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'No workflow runs yet'})).toBeInTheDocument();
        expect(screen.queryByRole('list', {name: 'Getting started'})).not.toBeInTheDocument();
    });
});
