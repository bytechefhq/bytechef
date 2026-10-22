import {TooltipProvider} from '@/components/ui/tooltip';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {InitialEntry, MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeployments from './ProjectDeployments';

const hoisted = vi.hoisted(() => ({
    projectDeployments: [] as ProjectDeployment[],
    refetchProjectDeployments: vi.fn(),
}));

const publishedProject: Project = {
    id: 4,
    lastPublishedDate: new Date('2026-10-01T10:00:00Z'),
    name: 'Order alerts',
    projectWorkflowIds: [41],
    workspaceId: 1,
};

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition: string}}) => unknown) =>
        selector({application: {edition: 'CE'}}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEnvironmentsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projects.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projects.queries')>()),
    useGetWorkspaceProjectsQuery: () => ({data: [publishedProject], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectDeployments.queries')>()),
    useGetWorkspaceProjectDeploymentsQuery: () => ({
        data: hoisted.projectDeployments,
        error: null,
        isFetching: false,
        isLoading: false,
        refetch: hoisted.refetchProjectDeployments,
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

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({onSuccess, triggerNode}: {onSuccess?: (deploymentId: number) => void; triggerNode: ReactNode}) => (
        <>
            {triggerNode}

            <button onClick={() => onSuccess?.(9)}>Finish deployment dialog</button>
        </>
    ),
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentList', () => ({
    default: ({newlyCreatedDeploymentId}: {newlyCreatedDeploymentId?: number}) => (
        <div>{`Expanded deployment: ${newlyCreatedDeploymentId ?? 'none'}`}</div>
    ),
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet',
    () => ({
        default: () => null,
    })
);

const renderProjectDeployments = (initialEntries: InitialEntry[] = ['/']) =>
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <TooltipProvider>
                <ProjectDeployments />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('ProjectDeployments empty states', () => {
    beforeEach(() => {
        hoisted.projectDeployments = [];
    });

    it('guides a first deployment and hides the project and tag sidebar', () => {
        renderProjectDeployments();

        expect(screen.getByRole('heading', {level: 2, name: 'Create your first deployment'})).toBeInTheDocument();
        expect(screen.queryByText('All Projects')).not.toBeInTheDocument();
        expect(screen.getByText('Deployments')).toBeInTheDocument();
    });

    it('keeps the sidebar and reports a filter miss when a filter matches nothing', () => {
        renderProjectDeployments(['/?tagId=1']);

        expect(screen.getByText('No Matching Project Deployments')).toBeInTheDocument();
        expect(screen.queryByText('Create your first deployment')).not.toBeInTheDocument();
        expect(screen.getByText('All Projects')).toBeInTheDocument();
    });
});

describe('ProjectDeployments expanded deployment', () => {
    beforeEach(() => {
        hoisted.projectDeployments = [
            {
                enabled: true,
                environmentId: 0,
                id: 2,
                name: 'Order alerts deployment',
                project: publishedProject,
                projectId: publishedProject.id,
                projectVersion: 1,
            },
        ];

        hoisted.refetchProjectDeployments.mockClear();
    });

    it('should expand nothing when no deployment is handed over', () => {
        renderProjectDeployments();

        expect(screen.getByText('Expanded deployment: none')).toBeInTheDocument();
    });

    it('should expand the deployment the workflow editor redirected to', () => {
        renderProjectDeployments([{pathname: '/', state: {projectDeploymentId: 2}}]);

        expect(screen.getByText('Expanded deployment: 2')).toBeInTheDocument();
    });

    it('should expand a deployment created from the header dialog over the redirected one', async () => {
        renderProjectDeployments([{pathname: '/', state: {projectDeploymentId: 2}}]);

        await userEvent.click(screen.getByRole('button', {name: 'Finish deployment dialog'}));

        expect(screen.getByText('Expanded deployment: 9')).toBeInTheDocument();
    });

    it('should refetch deployments in place and keep the expanded deployment', async () => {
        renderProjectDeployments([{pathname: '/', state: {projectDeploymentId: 2}}]);

        await userEvent.click(screen.getByRole('button', {name: 'Refresh deployments'}));

        expect(hoisted.refetchProjectDeployments).toHaveBeenCalledTimes(1);
        expect(screen.getByText('Expanded deployment: 2')).toBeInTheDocument();
    });
});
