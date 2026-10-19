import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import posthog from 'posthog-js';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentsEmptyState from './ProjectDeploymentsEmptyState';

interface QueryResultI<T> {
    data?: T;
    error: Error | null;
    isLoading: boolean;
}

const hoisted = vi.hoisted(() => ({
    projectDeploymentsResult: {data: [], error: null, isLoading: false} as QueryResultI<ProjectDeployment[]>,
    projectsResult: {data: [], error: null, isLoading: false} as QueryResultI<Project[]>,
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

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({triggerNode}: {triggerNode: ReactNode}) => triggerNode,
}));

const draftProject: Project = {id: 1, name: 'Lead sync', projectWorkflowIds: [], workspaceId: 1};

const draftProjectWithWorkflow: Project = {id: 2, name: 'Invoice digest', projectWorkflowIds: [21], workspaceId: 1};

const publishedProject: Project = {
    id: 4,
    lastPublishedDate: new Date('2026-10-01T10:00:00Z'),
    name: 'Order alerts',
    projectWorkflowIds: [41],
    workspaceId: 1,
};

const renderEmptyState = () =>
    render(
        <MemoryRouter>
            <ProjectDeploymentsEmptyState onDeploymentCreated={vi.fn()} />
        </MemoryRouter>
    );

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.projectDeploymentsResult = {data: [], error: null, isLoading: false};
    hoisted.projectsResult = {data: [], error: null, isLoading: false};
});

describe('ProjectDeploymentsEmptyState', () => {
    // The deployment dialog only lists published versions, so offering it here used to open a form nobody could submit.
    it('sends a user without a workflow to Projects instead of opening the deployment dialog', () => {
        hoisted.projectsResult = {data: [draftProject], error: null, isLoading: false};

        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'Nothing to deploy yet'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Go to Projects'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Deployment'})).not.toBeInTheDocument();
    });

    it('asks the user to publish their only project and offers to open it', () => {
        hoisted.projectsResult = {data: [draftProject, draftProjectWithWorkflow], error: null, isLoading: false};

        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'Publish a project to deploy it'})).toBeInTheDocument();
        expect(screen.getByText(/Open Invoice digest and click Publish/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Open Invoice digest'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Deployment'})).not.toBeInTheDocument();
    });

    it('offers to create a deployment once a version is published', () => {
        hoisted.projectsResult = {data: [publishedProject], error: null, isLoading: false};

        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'Create your first deployment'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Deployment'})).toBeInTheDocument();

        const steps = within(screen.getByRole('list', {name: 'Getting started'})).getAllByRole('listitem');

        expect(steps[3]).toHaveTextContent('Deploy and enable');
        expect(steps[3]).toHaveAttribute('aria-current', 'step');
    });

    it('records a click on the next step with the stage', async () => {
        hoisted.projectsResult = {data: [publishedProject], error: null, isLoading: false};

        renderEmptyState();

        await userEvent.click(screen.getByRole('button', {name: 'Create Deployment'}));

        await waitFor(() =>
            expect(posthog.capture).toHaveBeenCalledWith('empty_state_action_clicked', {
                action: 'create_deployment',
                page: 'deployments',
                stage: 'deploy',
            })
        );
    });

    it('links to the deployment docs', () => {
        renderEmptyState();

        expect(screen.getByRole('link', {name: 'How deployments work'})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/platform/automation/deploy/workflows'
        );
    });

    it('shows no state until the journey is known', () => {
        hoisted.projectsResult = {data: undefined, error: null, isLoading: true};

        renderEmptyState();

        expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    it('falls back to creating a deployment when the journey cannot be loaded', () => {
        hoisted.projectsResult = {data: undefined, error: new Error('Request failed'), isLoading: false};

        renderEmptyState();

        expect(screen.getByRole('heading', {level: 2, name: 'Create your first deployment'})).toBeInTheDocument();
        expect(screen.queryByRole('list', {name: 'Getting started'})).not.toBeInTheDocument();
    });
});
